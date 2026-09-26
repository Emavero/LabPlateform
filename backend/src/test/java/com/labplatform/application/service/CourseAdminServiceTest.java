package com.labplatform.application.service;

import com.labplatform.application.fakes.Fakes;
import com.labplatform.application.fakes.InMemoryBoxes;
import com.labplatform.application.fakes.InMemoryCompletions;
import com.labplatform.application.fakes.InMemoryCourses;
import com.labplatform.application.fakes.InMemoryOwns;
import com.labplatform.application.fakes.InMemoryUsers;
import com.labplatform.application.port.in.admin.AdminOverview;
import com.labplatform.application.port.in.admin.CourseDraft;
import com.labplatform.application.port.in.admin.CourseDraft.SectionDraft;
import com.labplatform.domain.academy.Course;
import com.labplatform.domain.academy.CourseLevel;
import com.labplatform.domain.academy.CourseSection;
import com.labplatform.domain.academy.SectionKind;
import com.labplatform.domain.academy.Track;
import com.labplatform.domain.shared.ConflictException;
import com.labplatform.domain.shared.ForbiddenException;
import com.labplatform.domain.shared.InvalidInputException;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseAdminServiceTest {

    private static final Actor ADMIN = new Actor(1L, Role.ADMIN);
    private static final Actor LEARNER = new Actor(2L, Role.USER);
    private static final Instant NOW = Instant.parse("2026-09-26T09:00:00Z");

    private InMemoryCourses courses;
    private InMemoryCompletions completions;
    private CourseAdminService admin;
    private AcademyService academy;

    @BeforeEach
    void setUp() {
        courses = new InMemoryCourses();
        completions = new InMemoryCompletions();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        admin = new CourseAdminService(courses, completions, new InMemoryUsers(), new InMemoryBoxes(),
                new InMemoryOwns(), Fakes.NO_TRANSACTION, clock);
        academy = new AcademyService(courses, completions, Fakes.NO_TRANSACTION, clock);
    }

    private static CourseDraft draft(String title, SectionDraft... sections) {
        return new CourseDraft(title, Track.DEFENSE, CourseLevel.EASY, "Résumé du cours.", List.of(sections));
    }

    private static SectionDraft section(Long id, String title) {
        return new SectionDraft(id, title, SectionKind.THEORY, 15, "Contenu.", null);
    }

    @Test
    void anAdminPublishesACourseWhoseUrlComesFromItsTitle() {
        Course created = admin.createCourse(ADMIN, draft("Répondre à un incident", section(null, "Contenir")));

        assertEquals("repondre-a-un-incident", created.getSlug());
        assertEquals("Répondre à un incident", created.getTitle());
        assertEquals(1, created.getSections().size());
        assertEquals("contenir", created.getSections().get(0).slug());
        assertEquals(1, created.getSections().get(0).position());
    }

    @Test
    void aLearnerCannotPublishModifyOrDeleteACourse() {
        Course course = admin.createCourse(ADMIN, draft("Durcir", section(null, "Inventaire")));

        assertThrows(ForbiddenException.class, () -> admin.createCourse(LEARNER, draft("Autre", section(null, "S"))));
        assertThrows(ForbiddenException.class,
                () -> admin.updateCourse(LEARNER, course.getSlug(), draft("Durcir", section(null, "S"))));
        assertThrows(ForbiddenException.class, () -> admin.deleteCourse(LEARNER, course.getSlug()));
        assertThrows(ForbiddenException.class, () -> admin.overview(LEARNER));
    }

    @Test
    void twoCoursesCannotShareTheSameUrl() {
        admin.createCourse(ADMIN, draft("Durcir", section(null, "Inventaire")));

        assertThrows(ConflictException.class, () -> admin.createCourse(ADMIN, draft("Durcir", section(null, "S"))));
    }

    @Test
    void twoSectionsOfTheSameTitleGetDistinctUrls() {
        Course created = admin.createCourse(ADMIN,
                draft("Durcir", section(null, "Atelier"), section(null, "Atelier")));

        assertEquals(List.of("atelier", "atelier-2"),
                created.getSections().stream().map(CourseSection::slug).toList());
    }

    @Test
    void aCourseWithoutSectionIsRefused() {
        assertThrows(InvalidInputException.class, () -> admin.createCourse(ADMIN, draft("Vide")));
    }

    @Test
    void renamingASectionKeepsTheProgressOfThoseWhoFinishedIt() {
        Course created = admin.createCourse(ADMIN, draft("Durcir", section(null, "Inventaire"), section(null, "Sudo")));
        academy.completeSection(LEARNER, created.getSlug(), "inventaire");
        Long keptId = created.getSections().get(0).id();

        Course updated = admin.updateCourse(ADMIN, created.getSlug(),
                draft("Durcir", section(keptId, "Inventaire des services"), section(null, "Nouveau")));

        // L'identifiant survit au renommage, donc la section reste cochée.
        assertEquals(keptId, updated.getSections().get(0).id());
        assertEquals("Inventaire des services", updated.getSections().get(0).title());
        assertEquals(1, academy.getCourse(LEARNER, created.getSlug()).progress().completedSections());
    }

    @Test
    void aSectionLeftOutOfTheDraftDisappears() {
        Course created = admin.createCourse(ADMIN, draft("Durcir", section(null, "Un"), section(null, "Deux")));
        Long kept = created.getSections().get(0).id();

        Course updated = admin.updateCourse(ADMIN, created.getSlug(), draft("Durcir", section(kept, "Un")));

        assertEquals(1, updated.getSections().size());
    }

    @Test
    void aSectionOfAnotherCourseIsRefused() {
        Course other = admin.createCourse(ADMIN, draft("Autre", section(null, "Section")));
        admin.createCourse(ADMIN, draft("Durcir", section(null, "Inventaire")));

        assertThrows(NotFoundException.class, () -> admin.updateCourse(ADMIN, "durcir",
                draft("Durcir", section(other.getSections().get(0).id(), "Volée"))));
    }

    @Test
    void theUrlOfACourseNeverChangesEvenRenamed() {
        Course created = admin.createCourse(ADMIN, draft("Durcir", section(null, "Inventaire")));

        Course renamed = admin.updateCourse(ADMIN, created.getSlug(),
                draft("Durcir un système exposé", section(created.getSections().get(0).id(), "Inventaire")));

        assertEquals("durcir", renamed.getSlug());
        assertEquals("Durcir un système exposé", renamed.getTitle());
    }

    @Test
    void aVideoOnlySectionIsAccepted() {
        Course created = admin.createCourse(ADMIN, new CourseDraft("Vidéo", Track.FORENSICS, CourseLevel.EASY, null,
                List.of(new SectionDraft(null, "Démonstration", SectionKind.THEORY, 8, null,
                        "https://www.youtube.com/watch?v=abc"))));

        CourseSection section = created.getSections().get(0);
        assertEquals("", section.content());
        assertTrue(section.hasVideo());
        assertNotNull(section.videoUrl());
    }

    @Test
    void aVideoAddressThatIsNotHttpIsRefused() {
        assertThrows(InvalidInputException.class, () -> admin.createCourse(ADMIN,
                new CourseDraft("Piégé", Track.FORENSICS, CourseLevel.EASY, null,
                        List.of(new SectionDraft(null, "Section", SectionKind.THEORY, 5, "Texte.",
                                "javascript:alert(1)")))));
    }

    @Test
    void anEmptyVideoAddressMeansNoVideo() {
        Course created = admin.createCourse(ADMIN, new CourseDraft("Sans vidéo", Track.DEFENSE, CourseLevel.EASY, null,
                List.of(new SectionDraft(null, "Section", SectionKind.THEORY, 5, "Texte.", "  "))));

        assertNull(created.getSections().get(0).videoUrl());
    }

    @Test
    void deletingACourseRemovesItFromTheCatalogue() {
        admin.createCourse(ADMIN, draft("Durcir", section(null, "Inventaire")));

        admin.deleteCourse(ADMIN, "durcir");

        assertEquals(0, academy.listCourses(LEARNER, null).size());
        assertThrows(NotFoundException.class, () -> admin.deleteCourse(ADMIN, "durcir"));
    }

    @Test
    void theOverviewCountsWhatIsPublishedAndWhatIsUsed() {
        admin.createCourse(ADMIN, draft("Durcir", section(null, "Un"), section(null, "Deux")));
        academy.completeSection(LEARNER, "durcir", "un");

        AdminOverview overview = admin.overview(ADMIN);

        assertEquals(1, overview.courses());
        assertEquals(2, overview.sections());
        assertEquals(1, overview.sectionsCompleted());
        assertEquals(0, overview.flagsValidated());
    }
}
