package com.labplatform.application.service;

import com.labplatform.application.fakes.Fakes;
import com.labplatform.application.fakes.InMemoryCompletions;
import com.labplatform.application.fakes.InMemoryCourses;
import com.labplatform.application.port.in.academy.CourseView;
import com.labplatform.application.port.in.academy.LearningProgress;
import com.labplatform.domain.academy.Course;
import com.labplatform.domain.academy.CourseLevel;
import com.labplatform.domain.academy.CourseSection;
import com.labplatform.domain.academy.SectionKind;
import com.labplatform.domain.academy.Track;
import com.labplatform.domain.shared.NotFoundException;
import com.labplatform.domain.user.Actor;
import com.labplatform.domain.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcademyServiceTest {

    private static final Actor ALICE = new Actor(1L, Role.USER);
    private static final Actor BOB = new Actor(2L, Role.USER);
    private static final Instant NOW = Instant.parse("2026-09-26T09:00:00Z");

    private InMemoryCourses courses;
    private InMemoryCompletions completions;
    private AcademyService academy;

    @BeforeEach
    void setUp() {
        courses = new InMemoryCourses();
        completions = new InMemoryCompletions();
        // Forensique : un cours de deux sections. Défense : un cours d'une section.
        courses.save(course("traces", "Traces", Track.FORENSICS, CourseLevel.FUNDAMENTAL, 2));
        courses.save(course("durcir", "Durcir", Track.DEFENSE, CourseLevel.MEDIUM, 1));
        academy = new AcademyService(courses, completions, Fakes.NO_TRANSACTION, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static Course course(String slug, String title, Track track, CourseLevel level, int sections) {
        List<CourseSection> content = java.util.stream.IntStream.rangeClosed(1, sections)
                .mapToObj(i -> new CourseSection(null, "section-" + i, "Section " + i, SectionKind.THEORY, i, 10,
                        "Contenu " + i))
                .toList();
        return Course.create(slug, title, track, level, "Résumé.", NOW.minusSeconds(86_400), content);
    }

    @Test
    void listsOnlyTheCoursesOfTheRequestedTrack() {
        assertEquals(List.of("traces"),
                academy.listCourses(ALICE, Track.FORENSICS).stream().map(v -> v.course().getSlug()).toList());
        assertEquals(List.of("durcir"),
                academy.listCourses(ALICE, Track.DEFENSE).stream().map(v -> v.course().getSlug()).toList());
        assertEquals(2, academy.listCourses(ALICE, null).size());
    }

    @Test
    void aFreshCourseIsNeitherStartedNorCompleted() {
        CourseView view = academy.getCourse(ALICE, "traces");

        assertEquals(0, view.progress().completedSections());
        assertEquals(2, view.progress().totalSections());
        assertFalse(view.progress().isStarted());
        assertFalse(view.progress().isCompleted());
        assertEquals(20, view.course().totalMinutes());
    }

    @Test
    void completingEverySectionCompletesTheCourse() {
        academy.completeSection(ALICE, "traces", "section-1");
        CourseView view = academy.completeSection(ALICE, "traces", "section-2");

        assertTrue(view.progress().isCompleted());
        assertEquals(1.0, view.progress().ratio(), 1e-9);
    }

    @Test
    void completingTwiceCountsOnce() {
        academy.completeSection(ALICE, "traces", "section-1");
        CourseView view = academy.completeSection(ALICE, "traces", "section-1");

        assertEquals(1, view.progress().completedSections());
        assertEquals(1, completions.count());
    }

    @Test
    void reopeningASectionUndoesIt() {
        academy.completeSection(ALICE, "traces", "section-1");

        CourseView view = academy.reopenSection(ALICE, "traces", "section-1");

        assertEquals(0, view.progress().completedSections());
        // Rouvrir une section jamais terminée ne casse rien.
        assertEquals(0, academy.reopenSection(ALICE, "traces", "section-2").progress().completedSections());
    }

    @Test
    void oneLearnersProgressIsInvisibleToAnother() {
        academy.completeSection(ALICE, "traces", "section-1");

        assertEquals(1, academy.getCourse(ALICE, "traces").progress().completedSections());
        assertEquals(0, academy.getCourse(BOB, "traces").progress().completedSections());
    }

    @Test
    void progressIsReportedTrackByTrack() {
        academy.completeSection(ALICE, "traces", "section-1");
        academy.completeSection(ALICE, "durcir", "section-1");

        List<LearningProgress> progress = academy.learningProgress(ALICE);

        LearningProgress forensics = progress.get(0);
        assertEquals(Track.FORENSICS, forensics.track());
        assertEquals(1, forensics.sectionsCompleted());
        assertEquals(2, forensics.sections());
        assertEquals(0, forensics.coursesCompleted());
        assertEquals(10, forensics.minutesDone());
        assertEquals(0.5, forensics.ratio(), 1e-9);

        LearningProgress defense = progress.get(1);
        assertEquals(Track.DEFENSE, defense.track());
        assertEquals(1, defense.coursesCompleted());
        assertEquals(1.0, defense.ratio(), 1e-9);
    }

    @Test
    void anUnknownCourseOrSectionIsNotFound() {
        assertThrows(NotFoundException.class, () -> academy.getCourse(ALICE, "inexistant"));
        assertThrows(NotFoundException.class, () -> academy.completeSection(ALICE, "traces", "section-9"));
        // La section d'un autre cours n'existe pas dans celui-ci.
        assertThrows(NotFoundException.class, () -> academy.completeSection(ALICE, "durcir", "section-2"));
    }

    @Test
    void trackSlugsAreResolvedAndUnknownOnesRejected() {
        assertEquals(Track.FORENSICS, Track.ofSlug("forensique"));
        assertEquals(Track.DEFENSE, Track.ofSlug("DEFENSE "));
        assertThrows(NotFoundException.class, () -> Track.ofSlug("crypto"));
    }
}
