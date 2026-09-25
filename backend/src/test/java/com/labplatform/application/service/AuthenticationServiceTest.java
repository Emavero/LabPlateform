package com.labplatform.application.service;

import com.labplatform.application.fakes.Fakes;
import com.labplatform.application.fakes.InMemoryUsers;
import com.labplatform.application.port.in.auth.AuthenticateUserUseCase;
import com.labplatform.application.port.in.auth.AuthenticatedSession;
import com.labplatform.application.port.in.auth.RegisterUserUseCase;
import com.labplatform.domain.shared.AuthenticationFailedException;
import com.labplatform.domain.shared.ConflictException;
import com.labplatform.domain.shared.InvalidInputException;
import com.labplatform.domain.user.Email;
import com.labplatform.domain.user.UserRegistered;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationServiceTest {

    private InMemoryUsers users;
    private List<Object> publishedEvents;
    private AuthenticationService auth;

    @BeforeEach
    void setUp() {
        users = new InMemoryUsers();
        publishedEvents = new ArrayList<>();
        Clock clock = Clock.fixed(Instant.parse("2026-09-24T10:00:00Z"), ZoneOffset.UTC);
        auth = new AuthenticationService(users, Fakes.REVERSING_HASHER, Fakes.TOKEN_ISSUER,
                publishedEvents::add, Fakes.NO_TRANSACTION, clock);
    }

    @Test
    void registrationCreatesTheAccountOpensASessionAndAnnouncesIt() {
        AuthenticatedSession session = auth.register(
                new RegisterUserUseCase.Command("New@Example.com", "password123", "password123"));

        assertEquals("new@example.com", session.user().email());
        assertEquals("token-for-" + session.user().id(), session.accessToken());
        assertEquals(List.of(new UserRegistered(session.user().id(), Email.of("new@example.com"))), publishedEvents);
        assertTrue(users.findByEmail(Email.of("new@example.com")).isPresent());
    }

    @Test
    void emailMustBeUniqueRegardlessOfCase() {
        auth.register(new RegisterUserUseCase.Command("dup@example.com", "password123", "password123"));

        assertThrows(ConflictException.class, () ->
                auth.register(new RegisterUserUseCase.Command("DUP@example.com", "password123", "password123")));
    }

    @Test
    void passwordConfirmationMustMatch() {
        assertThrows(InvalidInputException.class, () ->
                auth.register(new RegisterUserUseCase.Command("x@example.com", "password123", "password124")));
        assertTrue(publishedEvents.isEmpty());
    }

    @Test
    void loginSucceedsWithTheRightPasswordOnly() {
        auth.register(new RegisterUserUseCase.Command("me@example.com", "password123", "password123"));

        AuthenticatedSession session = auth.authenticate(new AuthenticateUserUseCase.Command("ME@example.com", "password123"));
        assertEquals("me@example.com", session.user().email());

        assertThrows(AuthenticationFailedException.class,
                () -> auth.authenticate(new AuthenticateUserUseCase.Command("me@example.com", "wrong-password")));
    }

    @Test
    void unknownOrMalformedEmailsFailLikeAWrongPassword() {
        assertThrows(AuthenticationFailedException.class,
                () -> auth.authenticate(new AuthenticateUserUseCase.Command("ghost@example.com", "password123")));
        assertThrows(AuthenticationFailedException.class,
                () -> auth.authenticate(new AuthenticateUserUseCase.Command("not-an-email", "password123")));
    }
}
