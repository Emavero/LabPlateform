package com.labplatform.application.service;

import com.labplatform.application.port.in.admin.AdminOverview;
import com.labplatform.application.port.in.admin.CourseDraft;
import com.labplatform.application.port.in.admin.GetAdminOverviewUseCase;
import com.labplatform.application.port.in.admin.ManageCoursesUseCase;
import com.labplatform.application.port.out.BoxRepositoryPort;
import com.labplatform.application.port.out.CourseRepositoryPort;
import com.labplatform.application.port.out.QuizRepositoryPort;
import com.labplatform.application.port.out.OwnRepositoryPort;
import com.labplatform.application.port.out.SectionCompletionRepositoryPort;
import com.labplatform.application.port.out.TransactionPort;
import com.labplatform.application.port.out.UserRepositoryPort;
import com.labplatform.domain.academy.Course;
import com.labplatform.domain.academy.CourseSection;
import com.labplatform.domain.academy.QuizChoice;
import com.labplatform.domain.academy.QuizQuestion;
import com.labplatform.domain.shared.ConflictException;
import com.labplatform.domain.shared.InvalidInputException;
import com.labplatform.domain.shared.NotFoundException;
import com.labplatform.domain.shared.Slug;
import com.labplatform.domain.user.Actor;
import com.labplatform.domain.user.AdminPolicy;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Publication des cours par un administrateur.
 * <p>
 * Deux règles portent tout le reste. L'identifiant d'URL d'un cours est
 * dérivé de son titre une fois pour toutes : renommer un cours ne casse pas
 * les liens déjà partagés. Une section conserve son identifiant tant que
 * l'éditeur le renvoie, donc la retitrer, la déplacer ou lui ajouter une
 * vidéo n'efface l'avancement de personne.
 */
public class CourseAdminService implements ManageCoursesUseCase, GetAdminOverviewUseCase {

    private static final int MAX_SECTIONS = 50;

    private final CourseRepositoryPort courses;
    private final SectionCompletionRepositoryPort completions;
    private final QuizRepositoryPort quizzes;
    private final UserRepositoryPort users;
    private final BoxRepositoryPort boxes;
    private final OwnRepositoryPort owns;
    private final TransactionPort transactions;
    private final Clock clock;

    public CourseAdminService(CourseRepositoryPort courses, SectionCompletionRepositoryPort completions,
                              QuizRepositoryPort quizzes, UserRepositoryPort users, BoxRepositoryPort boxes,
                              OwnRepositoryPort owns, TransactionPort transactions, Clock clock) {
        this.courses = courses;
        this.completions = completions;
        this.quizzes = quizzes;
        this.users = users;
        this.boxes = boxes;
        this.owns = owns;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public Course createCourse(Actor actor, CourseDraft draft) {
        AdminPolicy.requireAdmin(actor);
        requireSections(draft);

        String slug = Slug.from(draft.title());
        return transactions.inTransaction(() -> {
            if (courses.findBySlug(slug).isPresent()) {
                throw new ConflictException("Un cours porte déjà ce titre");
            }
            return saveQuizzes(courses.save(Course.create(slug, draft.title().trim(), draft.track(), draft.level(),
                    summaryOf(draft), clock.instant(), sectionsOf(draft, Map.of()))), draft);
        });
    }

    @Override
    public Course updateCourse(Actor actor, String slug, CourseDraft draft) {
        AdminPolicy.requireAdmin(actor);
        requireSections(draft);

        return transactions.inTransaction(() -> {
            Course existing = require(slug);
            // Les sections que l'éditeur renvoie avec leur identifiant sont les mêmes
            // qu'avant : elles gardent l'avancement des apprenants.
            Map<Long, CourseSection> known = existing.getSections().stream()
                    .collect(Collectors.toMap(CourseSection::id, Function.identity()));
            return saveQuizzes(courses.save(Course.restore(existing.getId(), existing.getSlug(),
                    draft.title().trim(), draft.track(), draft.level(), summaryOf(draft), existing.getPublishedAt(),
                    sectionsOf(draft, known))), draft);
        });
    }

    @Override
    public void deleteCourse(Actor actor, String slug) {
        AdminPolicy.requireAdmin(actor);
        transactions.inTransaction(() -> courses.delete(require(slug)));
    }

    @Override
    public AdminOverview overview(Actor actor) {
        AdminPolicy.requireAdmin(actor);
        List<Course> catalogue = courses.findAll();
        return new AdminOverview(
                users.count(),
                boxes.count(),
                catalogue.size(),
                catalogue.stream().mapToLong(course -> course.getSections().size()).sum(),
                owns.count(),
                completions.count());
    }

    /**
     * Les quiz sont enregistrés après le cours : leurs questions se rattachent
     * aux sections, qui viennent seulement d'obtenir leur identifiant.
     */
    private Course saveQuizzes(Course saved, CourseDraft draft) {
        List<CourseSection> sections = saved.getSections();
        for (int i = 0; i < sections.size() && i < draft.sections().size(); i++) {
            CourseSection section = sections.get(i);
            List<QuizQuestion> questions = questionsOf(draft.sections().get(i), section.id());
            quizzes.replaceForSection(section.id(), questions);
        }
        return saved;
    }

    private static List<QuizQuestion> questionsOf(CourseDraft.SectionDraft section, Long sectionId) {
        List<QuizQuestion> questions = new ArrayList<>();
        int position = 1;
        for (CourseDraft.QuestionDraft question : section.questions()) {
            List<QuizChoice> choices = new ArrayList<>();
            int choicePosition = 1;
            for (CourseDraft.ChoiceDraft choice : question.choices()) {
                choices.add(new QuizChoice(null, choice.label(), choice.correct(), choicePosition++));
            }
            questions.add(new QuizQuestion(null, sectionId, question.statement(), position++, choices));
        }
        return questions;
    }

    /** Les positions viennent de l'ordre de la liste, les identifiants d'URL des titres. */
    private static List<CourseSection> sectionsOf(CourseDraft draft, Map<Long, CourseSection> known) {
        Set<String> taken = new HashSet<>();
        List<CourseSection> sections = new ArrayList<>(draft.sections().size());
        int position = 1;
        for (CourseDraft.SectionDraft section : draft.sections()) {
            CourseSection previous = section.id() == null ? null : known.get(section.id());
            if (section.id() != null && previous == null) {
                throw new NotFoundException("Section introuvable dans ce cours");
            }
            String slug = previous != null ? previous.slug() : Slug.uniqueFrom(section.title(), taken);
            taken.add(slug);
            sections.add(new CourseSection(section.id(), slug, section.title().trim(), section.kind(), position++,
                    section.minutes(), contentOf(section), section.videoUrl()));
        }
        return sections;
    }

    private static void requireSections(CourseDraft draft) {
        if (draft.sections() == null || draft.sections().isEmpty()) {
            throw new InvalidInputException("Un cours comporte au moins une section");
        }
        if (draft.sections().size() > MAX_SECTIONS) {
            throw new InvalidInputException("Un cours ne peut pas dépasser " + MAX_SECTIONS + " sections");
        }
    }

    private static String contentOf(CourseDraft.SectionDraft section) {
        // Une section peut n'être qu'une vidéo : le texte est alors facultatif.
        return section.content() == null ? "" : section.content();
    }

    private static String summaryOf(CourseDraft draft) {
        return draft.summary() == null ? "" : draft.summary().trim();
    }

    private Course require(String slug) {
        return courses.findBySlug(slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new NotFoundException("Cours introuvable"));
    }
}
