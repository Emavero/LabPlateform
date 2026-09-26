package com.labplatform.adapter.in.web.dto;

import com.labplatform.application.port.in.academy.CourseView;
import com.labplatform.application.port.in.academy.LearningProgress;
import com.labplatform.domain.academy.Course;
import com.labplatform.domain.academy.CourseSection;
import com.labplatform.domain.academy.Track;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/** Représentations HTTP des cours et du suivi de lecture. */
public final class CourseDtos {

    private CourseDtos() {
    }

    /** Filière, telle que le menu et les pages de liste l'affichent. */
    public record TrackResponse(String track, String slug, String name, String description) {

        public static TrackResponse from(Track track) {
            return new TrackResponse(track.name(), track.slug(), track.displayName(), track.description());
        }

        public static List<TrackResponse> all() {
            return Arrays.stream(Track.values()).map(TrackResponse::from).toList();
        }
    }

    /** Cours sans le contenu des sections : de quoi remplir une liste. */
    public record CourseSummaryResponse(String slug, String title, String track, String trackName, String trackSlug,
                                        String level, String levelName, String summary, int sections,
                                        int sectionsCompleted, int minutes, boolean completed, boolean started,
                                        Instant publishedAt) {

        public static CourseSummaryResponse from(CourseView view) {
            Course course = view.course();
            return new CourseSummaryResponse(
                    course.getSlug(),
                    course.getTitle(),
                    course.getTrack().name(),
                    course.getTrack().displayName(),
                    course.getTrack().slug(),
                    course.getLevel().name(),
                    course.getLevel().displayName(),
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
                                 String level, String levelName, String summary, int minutes, int sectionsCompleted,
                                 boolean completed, Instant publishedAt, List<SectionResponse> sections) {

        public static CourseResponse from(CourseView view) {
            Course course = view.course();
            return new CourseResponse(
                    course.getSlug(),
                    course.getTitle(),
                    course.getTrack().name(),
                    course.getTrack().displayName(),
                    course.getTrack().slug(),
                    course.getLevel().name(),
                    course.getLevel().displayName(),
                    course.getSummary(),
                    course.totalMinutes(),
                    view.progress().completedSections(),
                    view.progress().isCompleted(),
                    course.getPublishedAt(),
                    course.getSections().stream()
                            .map(section -> SectionResponse.from(section, view.isCompleted(section)))
                            .toList());
        }
    }

    public record SectionResponse(String slug, String title, String kind, String kindName, int position, int minutes,
                                 String content, boolean completed) {

        static SectionResponse from(CourseSection section, boolean completed) {
            return new SectionResponse(section.slug(), section.title(), section.kind().name(),
                    section.kind().displayName(), section.position(), section.minutes(), section.content(),
                    completed);
        }
    }

    /** Avancement sur une filière entière. */
    public record LearningProgressResponse(String track, String trackName, String trackSlug, int courses,
                                           int coursesCompleted, int sections, int sectionsCompleted,
                                           int minutesDone, double ratio) {

        public static LearningProgressResponse from(LearningProgress progress) {
            return new LearningProgressResponse(progress.track().name(), progress.track().displayName(),
                    progress.track().slug(), progress.courses(), progress.coursesCompleted(), progress.sections(),
                    progress.sectionsCompleted(), progress.minutesDone(), progress.ratio());
        }
    }
}
