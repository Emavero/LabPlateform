package com.labplatform.application.service;

import com.labplatform.application.port.in.auth.RequestPasswordResetUseCase;
import com.labplatform.application.port.in.auth.ResetPasswordUseCase;
import com.labplatform.application.port.out.PasswordHasherPort;
import com.labplatform.application.port.out.PasswordResetNotifierPort;
import com.labplatform.application.port.out.SecretGeneratorPort;
import com.labplatform.application.port.out.TransactionPort;
import com.labplatform.application.port.out.UserRepositoryPort;
import com.labplatform.domain.shared.InvalidInputException;
import com.labplatform.domain.user.Email;
import com.labplatform.domain.user.PasswordPolicy;
import com.labplatform.domain.user.PasswordResetToken;
import com.labplatform.domain.user.User;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Cas d'usage « mot de passe oublié » et « réinitialiser le mot de passe ». */
public class PasswordResetService implements RequestPasswordResetUseCase, ResetPasswordUseCase {

    static final String GENERIC_MESSAGE =
            "Si un compte existe avec cette adresse, un lien de réinitialisation vient d'être envoyé.";

    private final UserRepositoryPort users;
    private final PasswordHasherPort passwordHasher;
    private final SecretGeneratorPort secrets;
    private final PasswordResetNotifierPort notifier;
    private final TransactionPort transactions;
    private final Clock clock;
    private final Duration tokenValidity;
    private final boolean exposeTokenForDemo;

    public PasswordResetService(UserRepositoryPort users, PasswordHasherPort passwordHasher,
                                SecretGeneratorPort secrets, PasswordResetNotifierPort notifier,
                                TransactionPort transactions, Clock clock, Duration tokenValidity,
                                boolean exposeTokenForDemo) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.secrets = secrets;
        this.notifier = notifier;
        this.transactions = transactions;
        this.clock = clock;
        this.tokenValidity = tokenValidity;
        this.exposeTokenForDemo = exposeTokenForDemo;
    }

    @Override
    public Result request(String rawEmail) {
        Email email = Email.of(rawEmail);

        Optional<String> issuedToken = transactions.inTransaction(() -> users.findByEmail(email).map(user -> {
            String rawToken = secrets.urlSafeToken();
            Instant expiresAt = clock.instant().plus(tokenValidity);
            user.assignResetToken(PasswordResetToken.fromRawToken(rawToken, expiresAt));
            users.save(user);
            notifier.sendResetLink(user.getEmail(), rawToken, expiresAt);
            return rawToken;
        }));

        // Réponse identique que le compte existe ou non : aucune énumération possible.
        return new Result(GENERIC_MESSAGE, exposeTokenForDemo ? issuedToken : Optional.empty());
    }

    @Override
    public void reset(ResetPasswordUseCase.Command command) {
        if (command.token() == null || command.token().isBlank()) {
            throw new InvalidInputException("Lien de réinitialisation invalide");
        }
        PasswordPolicy.validateNewPassword(command.newPassword(), command.confirmPassword());
        String newHash = passwordHasher.hash(command.newPassword());

        transactions.inTransaction(() -> {
            User user = users.findByResetTokenHash(PasswordResetToken.hashOf(command.token()))
                    .orElseThrow(() -> new InvalidInputException("Lien de réinitialisation invalide"));
            user.resetPassword(command.token(), newHash, clock.instant());
            users.save(user);
        });
    }
}
