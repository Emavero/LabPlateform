package com.labplatform.domain.box;

import com.labplatform.domain.lab.OperatingSystem;
import com.labplatform.domain.shared.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoxTest {

    private static final String USER_FLAG = "0123456789abcdef0123456789abcdef";
    private static final String ROOT_FLAG = "fedcba9876543210fedcba9876543210";
    private static final Instant NOW = Instant.parse("2026-09-25T12:00:00Z");

    private static Box box(Difficulty difficulty) {
        return Box.restore(7L, "mirage", "Mirage", OperatingSystem.LINUX, difficulty, "Portail interne.",
                "10.10.10.14", "cyberMans", NOW.minusSeconds(86_400), false,
                Flag.ofSecret(USER_FLAG), Flag.ofSecret(ROOT_FLAG));
    }

    @Test
    void acceptsTheRightFlagAndAwardsThePointsOfItsDifficulty() {
        Own own = box(Difficulty.MEDIUM).claim(3L, FlagKind.ROOT, ROOT_FLAG, false, NOW);

        assertEquals(FlagKind.ROOT, own.kind());
        assertEquals(18, own.points());
        assertEquals(3L, own.userId());
        assertEquals(7L, own.boxId());
        assertEquals(NOW, own.ownedAt());
        assertFalse(own.firstBlood());
    }

    @Test
    void rejectsTheFlagOfTheOtherPrivilegeLevel() {
        Box mirage = box(Difficulty.MEDIUM);

        InvalidInputException error =
                assertThrows(InvalidInputException.class, () -> mirage.claim(3L, FlagKind.USER, ROOT_FLAG, false, NOW));
        assertEquals("Flag incorrect", error.getMessage());
    }

    @Test
    void acceptsAFlagPastedWithSpacesUppercaseOrBraces() {
        Box mirage = box(Difficulty.EASY);

        assertEquals(8, mirage.claim(3L, FlagKind.USER, "  " + USER_FLAG.toUpperCase() + "  ", false, NOW).points());
        assertEquals(8, mirage.claim(3L, FlagKind.USER, "CYBM{" + USER_FLAG + "}", false, NOW).points());
    }

    @Test
    void refusesASubmissionThatIsNotAFlagAtAll() {
        Box mirage = box(Difficulty.EASY);

        assertThrows(InvalidInputException.class, () -> mirage.claim(3L, FlagKind.USER, "pas-un-flag", false, NOW));
        assertThrows(InvalidInputException.class, () -> mirage.claim(3L, FlagKind.USER, "", false, NOW));
    }

    @Test
    void splitsPointsBetweenUserAndRootFlags() {
        Box insane = box(Difficulty.INSANE);

        assertEquals(20, insane.pointsFor(FlagKind.USER));
        assertEquals(30, insane.pointsFor(FlagKind.ROOT));
        assertEquals(50, insane.totalPoints());
    }

    @Test
    void neverExposesTheFlagValue() {
        Flag flag = Flag.ofSecret(USER_FLAG);

        assertFalse(flag.toString().contains(USER_FLAG));
        assertFalse(flag.toString().contains(flag.hash()));
        assertTrue(flag.matches(USER_FLAG));
    }

    @Test
    void rejectsAnInvalidSlug() {
        assertThrows(InvalidInputException.class, () -> Box.create("Pas Un Slug", "Mirage", OperatingSystem.LINUX,
                Difficulty.EASY, "…", "10.10.10.14", "cyberMans", NOW,
                Flag.ofSecret(USER_FLAG), Flag.ofSecret(ROOT_FLAG)));
    }
}
