package com.labplatform.domain.user;

import com.labplatform.domain.shared.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-09-24T10:00:00Z");

    @Test
    void emailIsNormalized() {
        assertEquals("alice@example.com", Email.of("  Alice@Example.COM ").value());
    }

    @Test
    void malformedEmailsAreRejected() {
        assertThrows(InvalidInputException.class, () -> Email.of("alice"));
        assertThrows(InvalidInputException.class, () -> Email.of("alice@example"));
        assertThrows(InvalidInputException.class, () -> Email.of("a lice@example.com"));
        assertThrows(InvalidInputException.class, () -> Email.of(""));
        assertThrows(InvalidInputException.class, () -> Email.of(null));
    }

    @Test
    void passwordPolicyEnforcesLengthAndConfirmation() {
        PasswordPolicy.validateNewPassword("longenough", "longenough");

        assertThrows(InvalidInputException.class, () -> PasswordPolicy.validateNewPassword("short", "short"));
        assertThrows(InvalidInputException.class, () -> PasswordPolicy.validateNewPassword("longenough", "different"));
    }

    @Test
    void resetTokenIsSingleUse() {
        User user = User.register(Email.of("a@b.io"), "old-hash", NOW);
        user.assignResetToken(PasswordResetToken.fromRawToken("raw-token", NOW.plus(Duration.ofMinutes(15))));

        user.resetPassword("raw-token", "new-hash", NOW.plusSeconds(60));

        assertEquals("new-hash", user.getPasswordHash());
        assertTrue(user.getResetToken().isEmpty());
        assertThrows(InvalidInputException.class, () -> user.resetPassword("raw-token", "other", NOW.plusSeconds(61)));
    }

    @Test
    void expiredOrWrongResetTokenIsRejected() {
        User user = User.register(Email.of("a@b.io"), "old-hash", NOW);
        user.assignResetToken(PasswordResetToken.fromRawToken("raw-token", NOW.plus(Duration.ofMinutes(15))));

        assertThrows(InvalidInputException.class, () -> user.resetPassword("wrong", "new-hash", NOW));
        assertThrows(InvalidInputException.class,
                () -> user.resetPassword("raw-token", "new-hash", NOW.plus(Duration.ofMinutes(15))));
        assertEquals("old-hash", user.getPasswordHash());
    }

    @Test
    void onlyTheTokenHashIsStored() {
        PasswordResetToken token = PasswordResetToken.fromRawToken("raw-token", NOW);

        assertEquals(64, token.hash().length());
        assertTrue(!token.hash().contains("raw-token"));
    }
}
