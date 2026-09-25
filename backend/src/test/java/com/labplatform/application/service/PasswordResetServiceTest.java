package com.labplatform.application.service;

import com.labplatform.application.fakes.Fakes;
import com.labplatform.application.fakes.InMemoryUsers;
import com.labplatform.application.port.in.auth.RequestPasswordResetUseCase;
import com.labplatform.application.port.in.auth.ResetPasswordUseCase;
import com.labplatform.domain.shared.InvalidInputException;
import com.labplatform.domain.user.Email;
import com.labplatform.domain.user.User;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordResetServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-24T10:00:00Z"), ZoneOffset.UTC);

    private PasswordResetService service(InMemoryUsers users, List<String> sentTokens, boolean demo) {
        return new PasswordResetService(users, Fakes.REVERSING_HASHER, () -> "generated-token",
                (email, token, expiresAt) -> sentTokens.add(token), Fakes.NO_TRANSACTION, clock,
                Duration.ofMinutes(15), demo);
    }

    @Test
    void responseIsIdenticalWhetherOrNotTheAccountExists() {
        InMemoryUsers users = new InMemoryUsers();
        users.save(User.register(Email.of("known@example.com"), "h", clock.instant()));
        List<String> sent = new ArrayList<>();
        PasswordResetService service = service(users, sent, false);

        RequestPasswordResetUseCase.Result known = service.request("known@example.com");
        RequestPasswordResetUseCase.Result unknown = service.request("unknown@example.com");

        assertEquals(known, unknown);
        assertTrue(known.demoToken().isEmpty());
        assertEquals(List.of("generated-token"), sent);
    }

    @Test
    void fullResetFlowChangesThePasswordOnce() {
        InMemoryUsers users = new InMemoryUsers();
        User saved = users.save(User.register(Email.of("me@example.com"), "old", clock.instant()));
        PasswordResetService service = service(users, new ArrayList<>(), true);

        String token = service.request("me@example.com").demoToken().orElseThrow();
        service.reset(new ResetPasswordUseCase.Command(token, "brand-new-pw", "brand-new-pw"));

        assertEquals(Fakes.REVERSING_HASHER.hash("brand-new-pw"), users.findById(saved.getId()).orElseThrow().getPasswordHash());
        assertThrows(InvalidInputException.class,
                () -> service.reset(new ResetPasswordUseCase.Command(token, "another-pw1", "another-pw1")));
    }
}
