package com.labplatform.adapter.out.payment;

import com.labplatform.domain.shared.ForbiddenException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Vérification des notifications de paiement.
 * <p>
 * Stripe et Wave signent de la même façon : un en-tête
 * {@code t=<horodatage>,v1=<signature>} où la signature est le HMAC-SHA256 de
 * {@code <horodatage>.<corps brut>}. Le corps doit donc être comparé
 * <em>tel qu'il est arrivé</em> : le relire après désérialisation changerait un
 * espace ou l'ordre des champs et invaliderait une signature pourtant bonne.
 * <p>
 * L'horodatage est vérifié en plus de la signature : sans cela, une
 * notification interceptée pourrait être rejouée indéfiniment pour prolonger
 * un abonnement.
 */
final class SignedPayload {

    private static final String ALGORITHM = "HmacSHA256";
    private static final Duration TOLERANCE = Duration.ofMinutes(5);

    private SignedPayload() {
    }

    static void requireValid(String header, String rawBody, String secret, Instant now) {
        if (secret == null || secret.isBlank()) {
            throw new ForbiddenException("Notification refusée : aucune clé de vérification configurée");
        }
        if (header == null || header.isBlank()) {
            throw new ForbiddenException("Notification refusée : signature absente");
        }
        String timestamp = null;
        String signature = null;
        for (String part : header.split(",")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length != 2) {
                continue;
            }
            if ("t".equals(pair[0])) {
                timestamp = pair[1];
            } else if ("v1".equals(pair[0])) {
                signature = pair[1];
            }
        }
        if (timestamp == null || signature == null) {
            throw new ForbiddenException("Notification refusée : signature illisible");
        }
        requireFresh(timestamp, now);

        byte[] expected = hmac(secret, timestamp + "." + rawBody);
        byte[] received;
        try {
            received = HexFormat.of().parseHex(signature.trim());
        } catch (IllegalArgumentException notHex) {
            throw new ForbiddenException("Notification refusée : signature illisible");
        }
        // Comparaison à temps constant : une comparaison qui s'arrête au
        // premier octet différent laisse deviner la signature attendue.
        if (!MessageDigest.isEqual(expected, received)) {
            throw new ForbiddenException("Notification refusée : signature invalide");
        }
    }

    private static void requireFresh(String timestamp, Instant now) {
        Instant signedAt;
        try {
            signedAt = Instant.ofEpochSecond(Long.parseLong(timestamp.trim()));
        } catch (NumberFormatException notANumber) {
            throw new ForbiddenException("Notification refusée : horodatage illisible");
        }
        if (Duration.between(signedAt, now).abs().compareTo(TOLERANCE) > 0) {
            throw new ForbiddenException("Notification refusée : horodatage hors tolérance");
        }
    }

    private static byte[] hmac(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException impossible) {
            throw new IllegalStateException("HMAC-SHA256 indisponible", impossible);
        }
    }
}
