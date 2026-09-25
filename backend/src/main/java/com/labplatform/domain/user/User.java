package com.labplatform.domain.user;

import com.labplatform.domain.shared.InvalidInputException;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Agrégat utilisateur, sans aucune dépendance à un framework.
 * Le mot de passe n'y apparaît que sous forme d'empreinte : le hachage est
 * délégué à un port (PasswordHasherPort) côté application.
 */
public class User {

    private final Long id;
    private final Email email;
    private String passwordHash;
    private final Role role;
    private final Instant createdAt;
    private PasswordResetToken resetToken;

    private User(Long id, Email email, String passwordHash, Role role, Instant createdAt, PasswordResetToken resetToken) {
        this.id = id;
        this.email = Objects.requireNonNull(email, "email");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.role = Objects.requireNonNull(role, "role");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.resetToken = resetToken;
    }

    /** Nouveau compte (pas encore persisté). */
    public static User register(Email email, String passwordHash, Instant now) {
        return new User(null, email, passwordHash, Role.USER, now, null);
    }

    /** Reconstitution depuis la persistance. */
    public static User restore(Long id, Email email, String passwordHash, Role role, Instant createdAt,
                               PasswordResetToken resetToken) {
        return new User(Objects.requireNonNull(id, "id"), email, passwordHash, role, createdAt, resetToken);
    }

    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = Objects.requireNonNull(newPasswordHash, "newPasswordHash");
        // Un changement de mot de passe invalide toute demande de réinitialisation en cours.
        this.resetToken = null;
    }

    public void assignResetToken(PasswordResetToken token) {
        this.resetToken = Objects.requireNonNull(token, "token");
    }

    /**
     * Consomme le jeton de réinitialisation : il doit exister, correspondre
     * et ne pas être expiré. Usage unique.
     */
    public void resetPassword(String rawToken, String newPasswordHash, Instant now) {
        if (resetToken == null || rawToken == null
                || !resetToken.hash().equals(PasswordResetToken.hashOf(rawToken))) {
            throw new InvalidInputException("Lien de réinitialisation invalide");
        }
        if (resetToken.isExpiredAt(now)) {
            throw new InvalidInputException("Le lien de réinitialisation a expiré");
        }
        changePasswordHash(newPasswordHash);
    }

    public Long getId() {
        return id;
    }

    public Email getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Optional<PasswordResetToken> getResetToken() {
        return Optional.ofNullable(resetToken);
    }
}
