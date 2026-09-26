package com.labplatform.application.service;

import com.labplatform.application.fakes.Fakes;
import com.labplatform.application.fakes.InMemoryBoxes;
import com.labplatform.application.fakes.InMemoryOwns;
import com.labplatform.application.fakes.InMemoryRatings;
import com.labplatform.application.port.in.box.BoxView;
import com.labplatform.application.port.in.box.FlagSubmissionResult;
import com.labplatform.domain.box.Box;
import com.labplatform.domain.box.CommunityRating;
import com.labplatform.domain.box.Difficulty;
import com.labplatform.domain.box.Flag;
import com.labplatform.domain.box.FlagKind;
import com.labplatform.domain.lab.OperatingSystem;
import com.labplatform.domain.scoring.Rank;
import com.labplatform.domain.shared.ConflictException;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoxServiceTest {

    private static final Actor ALICE = new Actor(1L, Role.USER);
    private static final Actor BOB = new Actor(2L, Role.USER);
    private static final Instant NOW = Instant.parse("2026-09-25T12:00:00Z");

    private static final String SENTINEL_USER = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String SENTINEL_ROOT = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private static final String NORTHWIND_USER = "cccccccccccccccccccccccccccccccc";
    private static final String NORTHWIND_ROOT = "dddddddddddddddddddddddddddddddd";

    private InMemoryBoxes boxes;
    private InMemoryOwns owns;
    private InMemoryRatings ratings;
    private BoxService service;
    private ScoreboardService scoreboard;

    @BeforeEach
    void setUp() {
        boxes = new InMemoryBoxes();
        owns = new InMemoryOwns();
        ratings = new InMemoryRatings();
        // Sentinel : très facile (4 + 6). Northwind : facile (8 + 12). Catalogue = 30 points.
        boxes.save(newBox("sentinel", "Sentinel", Difficulty.VERY_EASY, SENTINEL_USER, SENTINEL_ROOT, 2));
        boxes.save(newBox("northwind", "Northwind", Difficulty.EASY, NORTHWIND_USER, NORTHWIND_ROOT, 1));
        scoreboard = new ScoreboardService(boxes, owns);
        service = new BoxService(boxes, owns, ratings, scoreboard, Fakes.NO_TRANSACTION,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static Box newBox(String slug, String name, Difficulty difficulty, String userFlag, String rootFlag,
                              int daysAgo) {
        return Box.create(slug, name, OperatingSystem.LINUX, difficulty, "Synopsis.", "10.10.10.1", "cyberMans",
                NOW.minusSeconds(daysAgo * 86_400L), Flag.ofSecret(userFlag), Flag.ofSecret(rootFlag));
    }

    @Test
    void listsTheCatalogueNewestFirstWithWhatThePlayerHasValidated() {
        service.submitFlag(ALICE, "sentinel", FlagKind.USER, SENTINEL_USER);

        List<BoxView> catalogue = service.listBoxes(ALICE);

        assertEquals(List.of("northwind", "sentinel"), catalogue.stream().map(v -> v.box().getSlug()).toList());
        BoxView sentinel = catalogue.get(1);
        assertTrue(sentinel.isOwned(FlagKind.USER));
        assertFalse(sentinel.isOwned(FlagKind.ROOT));
        assertFalse(sentinel.isPwned());
        assertEquals(4, sentinel.pointsEarned());
    }

    @Test
    void anotherPlayersProgressIsNeverVisible() {
        service.submitFlag(ALICE, "sentinel", FlagKind.USER, SENTINEL_USER);

        BoxView asSeenByBob = service.getBox(BOB, "sentinel");

        assertFalse(asSeenByBob.isOwned(FlagKind.USER));
        assertEquals(0, asSeenByBob.pointsEarned());
    }

    @Test
    void submittingTheUserFlagAwardsItsPointsAndUpdatesTheProgress() {
        FlagSubmissionResult result = service.submitFlag(ALICE, "northwind", FlagKind.USER, NORTHWIND_USER);

        assertEquals(FlagKind.USER, result.kind());
        assertEquals(8, result.pointsAwarded());
        assertFalse(result.pwned());
        assertEquals(8, result.progress().points());
        assertEquals(30, result.progress().availablePoints());
        assertEquals(Rank.HACKER, result.progress().rank());
    }

    @Test
    void bothFlagsMakeTheMachinePwned() {
        service.submitFlag(ALICE, "sentinel", FlagKind.USER, SENTINEL_USER);
        FlagSubmissionResult root = service.submitFlag(ALICE, "sentinel", FlagKind.ROOT, SENTINEL_ROOT);

        assertTrue(root.pwned());
        assertEquals(10, root.progress().points());
        assertEquals(1, root.progress().boxesPwned());
        assertTrue(service.getBox(ALICE, "sentinel").isPwned());
    }

    @Test
    void theFirstPlayerToValidateAFlagGetsTheFirstBlood() {
        FlagSubmissionResult first = service.submitFlag(ALICE, "sentinel", FlagKind.USER, SENTINEL_USER);
        FlagSubmissionResult second = service.submitFlag(BOB, "sentinel", FlagKind.USER, SENTINEL_USER);

        assertTrue(first.firstBlood());
        assertFalse(second.firstBlood());
        // Le retardataire touche les mêmes points : le first blood est une distinction, pas un bonus.
        assertEquals(first.pointsAwarded(), second.pointsAwarded());
        assertEquals(1, first.progress().firstBloods());
        assertEquals(0, second.progress().firstBloods());
    }

    @Test
    void thesameFlagCannotBeCountedTwice() {
        service.submitFlag(ALICE, "sentinel", FlagKind.USER, SENTINEL_USER);

        ConflictException error = assertThrows(ConflictException.class,
                () -> service.submitFlag(ALICE, "sentinel", FlagKind.USER, SENTINEL_USER));
        assertEquals("Vous avez déjà validé ce flag", error.getMessage());
        assertEquals(4, scoreboard.progressOf(ALICE).points());
    }

    @Test
    void aFlagFromAnotherMachineIsRefused() {
        assertThrows(InvalidInputException.class,
                () -> service.submitFlag(ALICE, "sentinel", FlagKind.USER, NORTHWIND_USER));
        assertEquals(0, scoreboard.progressOf(ALICE).points());
    }

    @Test
    void anUnknownMachineIsNotFound() {
        assertThrows(NotFoundException.class,
                () -> service.submitFlag(ALICE, "inexistante", FlagKind.USER, SENTINEL_USER));
        assertThrows(NotFoundException.class, () -> service.getBox(ALICE, "inexistante"));
    }

    @Test
    void aMalformedSubmissionIsRefusedBeforeAnyLookup() {
        assertThrows(InvalidInputException.class,
                () -> service.submitFlag(ALICE, "inexistante", FlagKind.USER, "trop court"));
    }

    @Test
    void aMachineIsRatedOnlyOnceItIsPwned() {
        service.submitFlag(ALICE, "sentinel", FlagKind.USER, SENTINEL_USER);

        assertThrows(ConflictException.class, () -> service.rateBox(ALICE, "sentinel", Difficulty.MEDIUM));

        service.submitFlag(ALICE, "sentinel", FlagKind.ROOT, SENTINEL_ROOT);
        BoxView rated = service.rateBox(ALICE, "sentinel", Difficulty.MEDIUM);

        assertEquals(Difficulty.MEDIUM, rated.myVote());
        assertEquals(1, rated.rating().votes());
    }

    @Test
    void thePerceivedDifficultyIsTheAverageOfTheVotes() {
        pwn(ALICE, "sentinel", SENTINEL_USER, SENTINEL_ROOT);
        pwn(BOB, "sentinel", SENTINEL_USER, SENTINEL_ROOT);
        service.rateBox(ALICE, "sentinel", Difficulty.EASY);
        service.rateBox(BOB, "sentinel", Difficulty.HARD);

        CommunityRating rating = service.getBox(ALICE, "sentinel").rating();

        // Facile (2) et Difficile (4) : la moyenne tombe sur Moyenne (3).
        assertEquals(2, rating.votes());
        assertEquals(3.0, rating.averageLevel(), 1e-9);
        assertEquals(Difficulty.MEDIUM, rating.perceived());
    }

    @Test
    void votingAgainReplacesThePreviousVote() {
        pwn(ALICE, "sentinel", SENTINEL_USER, SENTINEL_ROOT);
        service.rateBox(ALICE, "sentinel", Difficulty.EASY);

        BoxView revised = service.rateBox(ALICE, "sentinel", Difficulty.INSANE);

        assertEquals(1, revised.rating().votes());
        assertEquals(Difficulty.INSANE, revised.myVote());
    }

    @Test
    void anotherPlayersVoteIsNeverPresentedAsMine() {
        pwn(BOB, "sentinel", SENTINEL_USER, SENTINEL_ROOT);
        service.rateBox(BOB, "sentinel", Difficulty.HARD);

        BoxView asSeenByAlice = service.getBox(ALICE, "sentinel");

        assertNull(asSeenByAlice.myVote());
        assertEquals(1, asSeenByAlice.rating().votes());
    }

    private void pwn(Actor actor, String slug, String userFlag, String rootFlag) {
        service.submitFlag(actor, slug, FlagKind.USER, userFlag);
        service.submitFlag(actor, slug, FlagKind.ROOT, rootFlag);
    }
}
