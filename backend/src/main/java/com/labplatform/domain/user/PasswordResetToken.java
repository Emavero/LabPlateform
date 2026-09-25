package com.labplatform.domain.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Jeton de réinitialisation tel qu'il est stocké : seule son empreinte SHA-256
 * est conservée, jamais la valeur transmise à l'utilisateur. Une fuite de la
 * base ne permet donc pas de réinitialiser un compte.
 */
public record PasswordResetToken(String hash, Instant expiresAt) {

    public PasswordResetToken {
        Objects.requireNonNull(hash, "hash");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    public static PasswordResetToken fromRawToken(String rawToken, Instant expiresAt) {
        return new PasswordResetToken(hashOf(rawToken), expiresAt);
    }

    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public static String hashOf(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
