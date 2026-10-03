package com.labplatform.adapter.in.web.dto;

import com.labplatform.adapter.in.web.Texts;
import com.labplatform.application.port.in.academy.CourseView;
import com.labplatform.application.port.in.academy.LearningProgress;
import com.labplatform.domain.academy.AttackStage;
import com.labplatform.domain.academy.Course;
import com.labplatform.domain.academy.CourseBriefing;
import com.labplatform.domain.academy.CourseDesigner;
import com.labplatform.domain.academy.CourseSection;
import com.labplatform.domain.academy.CourseTopic;
import com.labplatform.domain.academy.RealWorldCase;
import com.labplatform.domain.academy.Quiz;
import com.labplatform.domain.academy.QuizQuestion;
import com.labplatform.domain.academy.QuizResult;
import com.labplatform.domain.academy.Track;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/** Représentations HTTP des cours et du suivi de lecture. */
public final class CourseDtos {

    private CourseDtos() {
    }

    /**
     * Filière, telle que le menu et les pages de liste l'affichent. Elle
     * transporte ses sous-domaines : les filtres de la page des cours sont
     * ainsi construits d'après le serveur, et non d'après une liste recopiée.
     */
    public record TrackResponse(String track, String slug, String name, String description,
                                List<TopicResponse> topics) {

        public static TrackResponse from(Track track) {
            return new TrackResponse(track.name(), track.slug(), Texts.of(track.displayName()), track.description(),
                    CourseTopic.of(track).stream().map(TopicResponse::from).toList());
        }

        public static List<TrackResponse> all() {
            return Arrays.stream(Track.values()).map(TrackResponse::from).toList();
        }
    }

    /** Sous-domaine d'une filière : un bouton de filtre sur la page des cours. */
    public record TopicResponse(String topic, String slug, String name, String track) {

        public static TopicResponse from(CourseTopic topic) {
            return new TopicResponse(topic.name(), topic.slug(), Texts.of(topic.displayName()),
                    topic.track().name());
        }
    }

    /** Cours sans le contenu des sections : de quoi remplir une liste. */
    public record CourseSummaryResponse(String slug, String title, String track, String trackName, String trackSlug,
                                        String topic, String topicName, String topicSlug,
                                        String level, String levelName, String summary, int sections,
                                        int sectionsCompleted, int minutes, boolean completed, boolean started,
                                        Instant publishedAt) {

        public static CourseSummaryResponse from(CourseView view) {
            Course course = view.course();
            return new CourseSummaryResponse(
                    course.getSlug(),
                    course.getTitle(),
                    course.getTrack().name(),
                    Texts.of(course.getTrack().displayName()),
                    course.getTrack().slug(),
                    course.getTopic().name(),
                    Texts.of(course.getTopic().displayName()),
                    course.getTopic().slug(),
                    course.getLevel().name(),
                    Texts.of(course.getLevel().displayName()),
                    course.getSummary(),
                    course.getSections().size(),
                    view.progress().completedSections(),
                    course.totalMinutes(),
                    view.progress().isCompleted(),
                    view.progress().isStarted(),
                    course.getPublishedAt());
        }
    }

    /** Cours complet : sections, contenu et état d'avancement. */
    public record CourseResponse(String slug, String title, String track, String trackName, String trackSlug,
                                 String topic, String topicName, String topicSlug,
                                 String level, String levelName, String summary, int minutes, int sectionsCompleted,
                                 boolean completed, Instant publishedAt, List<SectionResponse> sections,
                                 AttackPathResponse attackPath, RealCaseResponse realCase,
                                 List<DesignerResponse> designers) {

        public static CourseResponse from(CourseView view) {
            return from(view, false);
        }

        static CourseResponse from(CourseView view, boolean revealAnswers) {
            Course course = view.course();
            CourseBriefing briefing = course.getBriefing();
            return new CourseResponse(
                    course.getSlug(),
                    course.getTitle(),
                    course.getTrack().name(),
                    Texts.of(course.getTrack().displayName()),
                    course.getTrack().slug(),
                    course.getTopic().name(),
                    Texts.of(course.getTopic().displayName()),
                    course.getTopic().slug(),
                    course.getLevel().name(),
                    Texts.of(course.getLevel().displayName()),
                    course.getSummary(),
                    course.totalMinutes(),
                    view.progress().completedSections(),
                    view.progress().isCompleted(),
                    course.getPublishedAt(),
                    course.getSections().stream()
                            .map(section -> SectionResponse.from(section, view.isCompleted(section),
                                    view.quizOf(section), revealAnswers))
                            .toList(),
                    AttackPathResponse.from(briefing),
                    RealCaseResponse.from(briefing.realCase()),
                    briefing.designers().stream().map(DesignerResponse::from).toList());
        }
    }

    /** Chemin d'attaque. {@code null} quand le cours n'en décrit pas. */
    public record AttackPathResponse(String summary, List<StageResponse> stages) {

        static AttackPathResponse from(CourseBriefing briefing) {
            return briefing.attackPath().isPresent()
                    ? new AttackPathResponse(briefing.attackPath().summary(),
                            briefing.attackPath().stages().stream().map(StageResponse::from).toList())
                    : null;
        }
    }

    public record StageResponse(int position, String name, String description, String technique) {

        static StageResponse from(AttackStage stage) {
            return new StageResponse(stage.position(), stage.name(), stage.description(), stage.technique());
        }
    }

    /** Cas d'usage réel. {@code null} quand le cours n'en décrit pas. */
    public record RealCaseResponse(String sector, String situation, String stake, String outcome) {

        static RealCaseResponse from(RealWorldCase realCase) {
            return realCase.isPresent()
                    ? new RealCaseResponse(realCase.sector(), realCase.situation(), realCase.stake(),
                            realCase.outcome())
                    : null;
        }
    }

    /** Les initiales accompagnent l'avatar : la page sait présenter quelqu'un sans photo. */
    public record DesignerResponse(String name, String role, String avatarUrl, String initials) {

        static DesignerResponse from(CourseDesigner designer) {
            return new DesignerResponse(designer.name(), designer.role(), designer.avatarUrl(), designer.initials());
        }
    }

    /** L'identifiant sert à l'éditeur d'administration, qui le renvoie tel quel. */
    public record SectionResponse(Long id, String slug, String title, String kind, String kindName, int position,
                                  int minutes, String content, String videoUrl, boolean completed,
                                  List<QuestionResponse> questions) {

        static SectionResponse from(CourseSection section, boolean completed, Quiz quiz, boolean revealAnswers) {
            return new SectionResponse(section.id(), section.slug(), section.title(), section.kind().name(),
                    Texts.of(section.kind().displayName()), section.position(), section.minutes(), section.content(),
                    section.videoUrl(), completed,
                    quiz.questions().stream().map(question -> QuestionResponse.from(question, revealAnswers))
                            .toList());
        }
    }

    /**
     * Question servie à l'apprenant. Les bonnes réponses n'y figurent pas :
     * elles ne sortent qu'avec la correction, ou pour un administrateur.
     */
    public record QuestionResponse(Long id, String statement, int position, List<ChoiceResponse> choices) {

        static QuestionResponse from(QuizQuestion question, boolean revealAnswers) {
            return new QuestionResponse(question.id(), question.statement(), question.position(),
                    question.choices().stream()
                            .map(choice -> new ChoiceResponse(choice.id(), choice.label(),
                                    revealAnswers ? choice.correct() : null))
                            .toList());
        }
    }

    /** {@code correct} vaut null tant que la copie n'est pas rendue. */
    public record ChoiceResponse(Long id, String label, Boolean correct) {
    }

    /** Copie corrigée : score, réussite, et les bonnes réponses révélées. */
    public record QuizResultResponse(int correct, int questions, double ratio, boolean passed,
                                     List<AnswerResponse> answers) {

        public static QuizResultResponse from(QuizResult result) {
            return new QuizResultResponse(result.correct(), result.questions(), result.ratio(), result.isPassed(),
                    result.answers().stream()
                            .map(answer -> new AnswerResponse(answer.questionId(), answer.correct(),
                                    List.copyOf(answer.correctChoiceIds())))
                            .toList());
        }
    }

    public record AnswerResponse(Long questionId, boolean correct, List<Long> correctChoiceIds) {
    }

    /** Même fiche, mais les bonnes réponses visibles : réservée à l'administration. */
    public record AdminCourseResponse(CourseResponse course) {

        public static CourseResponse from(CourseView view) {
            return CourseResponse.from(view, true);
        }
    }

    /** Avancement sur une filière entière. */
    public record LearningProgressResponse(String track, String trackName, String trackSlug, int courses,
                                           int coursesCompleted, int sections, int sectionsCompleted,
                                           int minutesDone, double ratio) {

        public static LearningProgressResponse from(LearningProgress progress) {
            return new LearningProgressResponse(progress.track().name(), Texts.of(progress.track().displayName()),
                    progress.track().slug(), progress.courses(), progress.coursesCompleted(), progress.sections(),
                    progress.sectionsCompleted(), progress.minutesDone(), progress.ratio());
        }
    }
}
