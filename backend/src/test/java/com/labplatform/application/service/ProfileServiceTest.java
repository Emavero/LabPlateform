package com.labplatform.application.service;

import com.labplatform.application.fakes.Fakes;
import com.labplatform.application.fakes.InMemoryBoxes;
import com.labplatform.application.fakes.InMemoryCompletions;
import com.labplatform.application.fakes.InMemoryCourses;
import com.labplatform.application.fakes.InMemoryOwns;
import com.labplatform.application.fakes.InMemoryRatings;
import com.labplatform.application.port.in.profile.ActivityEntry;
import com.labplatform.domain.academy.Course;
import com.labplatform.domain.academy.CourseLevel;
import com.labplatform.domain.academy.CourseSection;
import com.labplatform.domain.academy.SectionKind;
import com.labplatform.domain.academy.Track;
import com.labplatform.domain.achievement.Achievement;
import com.labplatform.domain.box.Box;
import com.labplatform.domain.box.Difficulty;
import com.labplatform.domain.box.Flag;
import com.labplatform.domain.box.FlagKind;
import com.labplatform.domain.lab.OperatingSystem;
import com.labplatform.domain.user.Actor;
import com.labplatform.domain.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileServiceTest {

    private static final Actor ALICE = new Actor(1L, Role.USER);
    private static final Instant NOW = Instant.parse("2026-09-26T09:00:00Z");

    private static final String EASY_USER = "11111111111111111111111111111111";
    private static final String EASY_ROOT = "22222222222222222222222222222222";
    private static final String INSANE_USER = "33333333333333333333333333333333";
    private static final String INSANE_ROOT = "44444444444444444444444444444444";

    private BoxService catalogue;
    private AcademyService academy;
    private ProfileService profile;

    @BeforeEach
    void setUp() {
        InMemoryBoxes boxes = new InMemoryBoxes();
        InMemoryOwns owns = new InMemoryOwns();
        InMemoryCourses courses = new InMemoryCourses();
        InMemoryCompletions completions = new InMemoryCompletions();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

        boxes.save(box("cobalt", "Cobalt", Difficulty.EASY, EASY_USER, EASY_ROOT));
        boxes.save(box("obsidian", "Obsidian", Difficulty.INSANE, INSANE_USER, INSANE_ROOT));
        courses.save(course("traces", Track.FORENSICS));
        courses.save(course("durcir", Track.DEFENSE));

        ScoreboardService scoreboard = new ScoreboardService(boxes, owns);
        catalogue = new BoxService(boxes, owns, new InMemoryRatings(), scoreboard, Fakes.NO_TRANSACTION, clock);
        academy = new AcademyService(courses, completions, Fakes.NO_TRANSACTION, clock);
        profile = new ProfileService(boxes, owns, courses, completions);
    }

    private static Box box(String slug, String name, Difficulty difficulty, String userFlag, String rootFlag) {
        return Box.create(slug, name, OperatingSystem.LINUX, difficulty, "Synopsis.", "10.10.10.1", "cyberMans",
                NOW.minusSeconds(86_400), Flag.ofSecret(userFlag), Flag.ofSecret(rootFlag));
    }

    private static Course course(String slug, Track track) {
        return Course.create(slug, slug, track, CourseLevel.FUNDAMENTAL, "Résumé.", NOW.minusSeconds(86_400),
                List.of(new CourseSection(null, "section-1", "Section 1", SectionKind.THEORY, 1, 10, "Contenu")));
    }

    private Map<Achievement, Boolean> earnedByAlice() {
        return profile.achievements(ALICE).stream()
                .collect(Collectors.toMap(Achievement.Earned::achievement, Achievement.Earned::earned));
    }

    @Test
    void aNewPlayerHasNoAchievementButSeesThemAll() {
        List<Achievement.Earned> achievements = profile.achievements(ALICE);

        assertEquals(Achievement.values().length, achievements.size());
        assertTrue(achievements.stream().noneMatch(Achievement.Earned::earned));
    }

    @Test
    void validatingAFlagUnlocksTheFirstAchievements() {
        catalogue.submitFlag(ALICE, "cobalt", FlagKind.USER, EASY_USER);

        Map<Achievement, Boolean> earned = earnedByAlice();

        assertTrue(earned.get(Achievement.FIRST_FLAG));
        assertTrue(earned.get(Achievement.FIRST_BLOOD));
        assertFalse(earned.get(Achievement.FIRST_PWN));
    }

    @Test
    void theHardestMachineOwnedUnlocksTheDifficultyAchievements() {
        catalogue.submitFlag(ALICE, "cobalt", FlagKind.USER, EASY_USER);
        catalogue.submitFlag(ALICE, "cobalt", FlagKind.ROOT, EASY_ROOT);

        assertTrue(earnedByAlice().get(Achievement.FIRST_PWN));
        assertFalse(earnedByAlice().get(Achievement.HARD_PWN));

        catalogue.submitFlag(ALICE, "obsidian", FlagKind.USER, INSANE_USER);
        catalogue.submitFlag(ALICE, "obsidian", FlagKind.ROOT, INSANE_ROOT);

        // Insane dépasse le palier « difficile », donc les deux hauts faits tombent.
        assertTrue(earnedByAlice().get(Achievement.HARD_PWN));
        assertTrue(earnedByAlice().get(Achievement.INSANE_PWN));
    }

    @Test
    void workingBothTracksUnlocksTheLearningAchievements() {
        academy.completeSection(ALICE, "traces", "section-1");

        assertTrue(earnedByAlice().get(Achievement.STUDENT));
        assertTrue(earnedByAlice().get(Achievement.GRADUATE));
        assertFalse(earnedByAlice().get(Achievement.BOTH_TRACKS));

        academy.completeSection(ALICE, "durcir", "section-1");

        assertTrue(earnedByAlice().get(Achievement.BOTH_TRACKS));
    }

    @Test
    void activityMixesFlagsAndSectionsNewestFirst() {
        catalogue.submitFlag(ALICE, "cobalt", FlagKind.USER, EASY_USER);
        academy.completeSection(ALICE, "traces", "section-1");

        List<ActivityEntry> activity = profile.activity(ALICE, 10);

        assertEquals(2, activity.size());
        assertTrue(activity.stream().anyMatch(entry -> entry.kind() == ActivityEntry.Kind.FLAG
                && entry.title().equals("Cobalt") && entry.points() == 8 && entry.firstBlood()));
        assertTrue(activity.stream().anyMatch(entry -> entry.kind() == ActivityEntry.Kind.SECTION
                && entry.title().equals("Section 1") && entry.points() == 0));
    }

    @Test
    void activitySizeIsBounded() {
        catalogue.submitFlag(ALICE, "cobalt", FlagKind.USER, EASY_USER);
        catalogue.submitFlag(ALICE, "cobalt", FlagKind.ROOT, EASY_ROOT);

        assertEquals(1, profile.activity(ALICE, 0).size());
        assertEquals(2, profile.activity(ALICE, 10_000).size());
    }
}
