package com.labplatform.application.service;

import com.labplatform.application.port.in.academy.CourseView;
import com.labplatform.application.port.in.academy.GetCourseUseCase;
import com.labplatform.application.port.in.academy.GetLearningProgressUseCase;
import com.labplatform.application.port.in.academy.LearningProgress;
import com.labplatform.application.port.in.academy.ListCoursesUseCase;
import com.labplatform.application.port.in.academy.TrackSectionProgressUseCase;
import com.labplatform.application.port.out.CourseRepositoryPort;
import com.labplatform.application.port.out.SectionCompletionRepositoryPort;
import com.labplatform.application.port.out.TransactionPort;
import com.labplatform.domain.academy.Course;
import com.labplatform.domain.academy.CourseSection;
import com.labplatform.domain.academy.SectionCompletion;
import com.labplatform.domain.academy.Track;
import com.labplatform.domain.shared.NotFoundException;
import com.labplatform.domain.user.Actor;

import java.time.Clock;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cours et suivi de lecture.
 * <p>
 * Le service ne décide pas de ce qu'« avancer » veut dire : il rassemble les
 * sections cochées et laisse l'agrégat {@link Course} en tirer l'avancement.
 */
public class AcademyService implements ListCoursesUseCase, GetCourseUseCase, TrackSectionProgressUseCase,
        GetLearningProgressUseCase {

    /** Les plus accessibles d'abord : on entre dans une filière par son premier palier. */
    private static final Comparator<Course> DISPLAY_ORDER =
            Comparator.comparingInt((Course course) -> course.getLevel().ordinal()).thenComparing(Course::getTitle);

    private final CourseRepositoryPort courses;
    private final SectionCompletionRepositoryPort completions;
    private final TransactionPort transactions;
    private final Clock clock;

    public AcademyService(CourseRepositoryPort courses, SectionCompletionRepositoryPort completions,
                          TransactionPort transactions, Clock clock) {
        this.courses = courses;
        this.completions = completions;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public List<CourseView> listCourses(Actor actor, Track track) {
        Set<Long> done = completedSectionIds(actor);
        List<Course> catalogue = track == null ? courses.findAll() : courses.findByTrack(track);
        return catalogue.stream().sorted(DISPLAY_ORDER).map(course -> view(course, done)).toList();
    }

    @Override
    public CourseView getCourse(Actor actor, String slug) {
        return view(require(slug), completedSectionIds(actor));
    }

    @Override
    public CourseView completeSection(Actor actor, String courseSlug, String sectionSlug) {
        Course course = require(courseSlug);
        CourseSection section = course.requireSection(sectionSlug);

        transactions.inTransaction(() -> {
            if (!completions.exists(actor.userId(), section.id())) {
                completions.save(SectionCompletion.record(actor.userId(), course.getId(), section.id(),
                        clock.instant()));
            }
        });
        return view(course, completedSectionIds(actor));
    }

    @Override
    public CourseView reopenSection(Actor actor, String courseSlug, String sectionSlug) {
        Course course = require(courseSlug);
        CourseSection section = course.requireSection(sectionSlug);

        transactions.inTransaction(() -> completions.delete(actor.userId(), section.id()));
        return view(course, completedSectionIds(actor));
    }

    @Override
    public List<LearningProgress> learningProgress(Actor actor) {
        Set<Long> done = completedSectionIds(actor);
        List<Course> catalogue = courses.findAll();
        return Arrays.stream(Track.values())
                .map(track -> progressOf(track, catalogue, done))
                .toList();
    }

    private LearningProgress progressOf(Track track, List<Course> catalogue, Set<Long> done) {
        List<Course> ofTrack = catalogue.stream().filter(course -> course.getTrack() == track).toList();
        int sections = ofTrack.stream().mapToInt(course -> course.getSections().size()).sum();
        int sectionsDone = (int) ofTrack.stream()
                .flatMap(course -> course.getSections().stream())
                .filter(section -> done.contains(section.id()))
                .count();
        int coursesDone = (int) ofTrack.stream()
                .filter(course -> course.progressOf(done).isCompleted())
                .count();
        int minutesDone = ofTrack.stream()
                .flatMap(course -> course.getSections().stream())
                .filter(section -> done.contains(section.id()))
                .mapToInt(CourseSection::minutes)
                .sum();
        return new LearningProgress(track, ofTrack.size(), coursesDone, sections, sectionsDone, minutesDone);
    }

    private CourseView view(Course course, Set<Long> completedSectionIds) {
        return new CourseView(course, course.progressOf(completedSectionIds), completedSectionIds);
    }

    private Set<Long> completedSectionIds(Actor actor) {
        return completions.findByUser(actor.userId()).stream()
                .map(SectionCompletion::sectionId)
                .collect(Collectors.toSet());
    }

    private Course require(String slug) {
        return courses.findBySlug(slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new NotFoundException("Cours introuvable"));
    }
}
