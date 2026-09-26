package com.labplatform.application.service;

import com.labplatform.application.fakes.Fakes;
import com.labplatform.application.fakes.InMemoryBoxes;
import com.labplatform.application.fakes.InMemoryOwns;
import com.labplatform.application.fakes.InMemoryRatings;
import com.labplatform.application.port.in.scoring.LeaderboardEntry;
import com.labplatform.domain.box.Box;
import com.labplatform.domain.box.Difficulty;
import com.labplatform.domain.box.Flag;
import com.labplatform.domain.box.FlagKind;
import com.labplatform.domain.lab.OperatingSystem;
import com.labplatform.domain.scoring.PlayerProgress;
import com.labplatform.domain.scoring.Rank;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreboardServiceTest {

    private static final Actor ALICE = new Actor(1L, Role.USER);
    private static final Actor BOB = new Actor(2L, Role.USER);
    private static final Instant NOW = Instant.parse("2026-09-25T12:00:00Z");

    private static final String EASY_USER = "11111111111111111111111111111111";
    private static final String EASY_ROOT = "22222222222222222222222222222222";
    private static final String HARD_USER = "33333333333333333333333333333333";
    private static final String HARD_ROOT = "44444444444444444444444444444444";

    private InMemoryOwns owns;
    private BoxService catalogue;
    private ScoreboardService scoreboard;

    @BeforeEach
    void setUp() {
        InMemoryBoxes boxes = new InMemoryBoxes();
        owns = new InMemoryOwns();
        InMemoryRatings ratings = new InMemoryRatings();
        // Catalogue : 20 + 40 = 60 points, 4 flags.
        boxes.save(box(boxes, "cobalt", Difficulty.EASY, EASY_USER, EASY_ROOT));
        boxes.save(box(boxes, "blackice", Difficulty.HARD, HARD_USER, HARD_ROOT));
        scoreboard = new ScoreboardService(boxes, owns);
        catalogue = new BoxService(boxes, owns, ratings, scoreboard, Fakes.NO_TRANSACTION, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static Box box(InMemoryBoxes boxes, String slug, Difficulty difficulty, String userFlag, String rootFlag) {
        return Box.create(slug, slug, OperatingSystem.LINUX, difficulty, "Synopsis.", "10.10.10.1", "cyberMans",
                NOW, Flag.ofSecret(userFlag), Flag.ofSecret(rootFlag));
    }

    @Test
    void anEmptyProgressStillDescribesTheCatalogue() {
        PlayerProgress progress = scoreboard.progressOf(ALICE);

        assertEquals(0, progress.points());
        assertEquals(60, progress.availablePoints());
        assertEquals(4, progress.totalFlags());
        assertEquals(Rank.NOOB, progress.rank());
        assertEquals(Rank.SCRIPT_KIDDIE, progress.nextRank());
        assertEquals(3, progress.pointsToNextRank());
    }

    @Test
    void progressCountsFlagsPwnedMachinesAndFirstBloods() {
        catalogue.submitFlag(ALICE, "cobalt", FlagKind.USER, EASY_USER);
        catalogue.submitFlag(ALICE, "cobalt", FlagKind.ROOT, EASY_ROOT);
        catalogue.submitFlag(ALICE, "blackice", FlagKind.USER, HARD_USER);

        PlayerProgress progress = scoreboard.progressOf(ALICE);

        assertEquals(36, progress.points());
        assertEquals(3, progress.ownedFlags());
        assertEquals(1, progress.boxesPwned());
        assertEquals(3, progress.firstBloods());
        assertEquals(Rank.ELITE_HACKER, progress.rank());
    }

    @Test
    void theLeaderboardRanksByPointsAndMarksTheCallersRow() {
        catalogue.submitFlag(BOB, "cobalt", FlagKind.USER, EASY_USER);
        catalogue.submitFlag(ALICE, "blackice", FlagKind.USER, HARD_USER);
        catalogue.submitFlag(ALICE, "blackice", FlagKind.ROOT, HARD_ROOT);

        List<LeaderboardEntry> board = scoreboard.leaderboard(ALICE, 10);

        assertEquals(2, board.size());
        assertEquals(1, board.get(0).position());
        assertEquals(40, board.get(0).points());
        assertTrue(board.get(0).self());
        assertEquals(2, board.get(1).position());
        assertEquals(8, board.get(1).points());
        assertFalse(board.get(1).self());
    }

    @Test
    void theLeaderboardShowsAHandleAndNeverAFullEmail() {
        catalogue.submitFlag(BOB, "cobalt", FlagKind.USER, EASY_USER);

        LeaderboardEntry entry = scoreboard.leaderboard(ALICE, 10).get(0);

        assertEquals("joueur2", entry.handle());
        assertFalse(entry.handle().contains("@"));
    }

    @Test
    void theRequestedSizeIsBounded() {
        catalogue.submitFlag(ALICE, "cobalt", FlagKind.USER, EASY_USER);

        assertEquals(1, scoreboard.leaderboard(ALICE, 0).size());
        assertEquals(1, scoreboard.leaderboard(ALICE, 10_000).size());
    }
}
