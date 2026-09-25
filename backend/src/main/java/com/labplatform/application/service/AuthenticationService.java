package com.labplatform.application.service;

import com.labplatform.application.port.in.auth.AuthenticateUserUseCase;
import com.labplatform.application.port.in.auth.AuthenticatedSession;
import com.labplatform.application.port.in.auth.RegisterUserUseCase;
import com.labplatform.application.port.in.auth.UserSummary;
import com.labplatform.application.port.out.AccessTokenIssuerPort;
import com.labplatform.application.port.out.DomainEventPublisherPort;
import com.labplatform.application.port.out.PasswordHasherPort;
import com.labplatform.application.port.out.TransactionPort;
import com.labplatform.application.port.out.UserRepositoryPort;
import com.labplatform.domain.shared.AuthenticationFailedException;
import com.labplatform.domain.shared.ConflictException;
import com.labplatform.domain.shared.InvalidInputException;
import com.labplatform.domain.user.Email;
import com.labplatform.domain.user.PasswordPolicy;
import com.labplatform.domain.user.User;
import com.labplatform.domain.user.UserRegistered;

import java.time.Clock;
import java.util.Optional;

/** Cas d'usage d'inscription et de connexion. */
public class AuthenticationService implements RegisterUserUseCase, AuthenticateUserUseCase {

    private final UserRepositoryPort users;
    private final PasswordHasherPort passwordHasher;
    private final AccessTokenIssuerPort tokenIssuer;
    private final DomainEventPublisherPort events;
    private final TransactionPort transactions;
    private final Clock clock;

    /**
     * Empreinte factice comparée lorsque l'e-mail est inconnu : la connexion
     * prend alors le même temps que pour un compte existant, ce qui empêche
     * de deviner quels e-mails sont inscrits en mesurant les temps de réponse.
     */
    private final String timingDummyHash;

    public AuthenticationService(UserRepositoryPort users, PasswordHasherPort passwordHasher,
                                 AccessTokenIssuerPort tokenIssuer, DomainEventPublisherPort events,
                                 TransactionPort transactions, Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.events = events;
        this.transactions = transactions;
        this.clock = clock;
        this.timingDummyHash = passwordHasher.hash("timing-equalizer-not-a-real-password");
    }

    @Override
    public AuthenticatedSession register(RegisterUserUseCase.Command command) {
        Email email = Email.of(command.email());
        PasswordPolicy.validateNewPassword(command.password(), command.confirmPassword());
        String passwordHash = passwordHasher.hash(command.password());

        User saved = transactions.inTransaction(() -> {
            if (users.existsByEmail(email)) {
                throw new ConflictException("Un compte existe déjà avec cette adresse e-mail");
            }
            User user = users.save(User.register(email, passwordHash, clock.instant()));
            // Publié dans la transaction : le lab est provisionné atomiquement avec le compte.
            events.publish(new UserRegistered(user.getId(), user.getEmail()));
            return user;
        });

        return openSession(saved);
    }

    @Override
    public AuthenticatedSession authenticate(AuthenticateUserUseCase.Command command) {
        if (command.password() == null || command.password().isEmpty()) {
            throw new AuthenticationFailedException();
        }

        Optional<User> user;
        try {
            user = users.findByEmail(Email.of(command.email()));
        } catch (InvalidInputException malformedEmail) {
            user = Optional.empty();
        }

        String hashToCheck = user.map(User::getPasswordHash).orElse(timingDummyHash);
        boolean passwordMatches = passwordHasher.matches(command.password(), hashToCheck);

        if (user.isEmpty() || !passwordMatches) {
            throw new AuthenticationFailedException();
        }
        return openSession(user.get());
    }

    private AuthenticatedSession openSession(User user) {
        UserSummary summary = UserSummary.of(user);
        return new AuthenticatedSession(summary, tokenIssuer.issue(summary));
    }
}
