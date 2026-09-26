package com.labplatform.adapter.out.payment;

import com.labplatform.domain.shared.ForbiddenException;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Ce test garde la seule porte par laquelle un tiers peut écrire dans la
 * facturation. S'il tombe, n'importe qui peut s'offrir un abonnement en
 * appelant l'URL de notification.
 */
class SignedPayloadTest {

    private static final String SECRET = "clé-de-vérification-du-prestataire";
    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final String BODY = "{\"type\":\"checkout.session.completed\"}";

    @Test
    void acceptsABodySignedWithTheSharedKey() {
        assertDoesNotThrow(() -> SignedPayload.requireValid(header(NOW, BODY, SECRET), BODY, SECRET, NOW));
    }

    @Test
    void refusesABodySignedWithAnotherKey() {
        String forged = header(NOW, BODY, "clé-de-l-attaquant");

        assertThrows(ForbiddenException.class, () -> SignedPayload.requireValid(forged, BODY, SECRET, NOW));
    }

    /** Un octet changé dans le corps invalide la signature : c'est tout l'intérêt. */
    @Test
    void refusesABodyAlteredAfterSigning() {
        String signature = header(NOW, BODY, SECRET);
        String altered = BODY.replace("completed", "expired");

        assertThrows(ForbiddenException.class, () -> SignedPayload.requireValid(signature, altered, SECRET, NOW));
    }

    /** Sans fenêtre de tolérance, une notification captée se rejouerait indéfiniment. */
    @Test
    void refusesAReplayOutsideTheToleranceWindow() {
        String old = header(NOW.minusSeconds(3_600), BODY, SECRET);

        assertThrows(ForbiddenException.class, () -> SignedPayload.requireValid(old, BODY, SECRET, NOW));
    }

    @Test
    void refusesAnAbsentMalformedOrUnsignableRequest() {
        assertThrows(ForbiddenException.class, () -> SignedPayload.requireValid(null, BODY, SECRET, NOW));
        assertThrows(ForbiddenException.class, () -> SignedPayload.requireValid("n'importe quoi", BODY, SECRET, NOW));
        assertThrows(ForbiddenException.class,
                () -> SignedPayload.requireValid("t=1,v1=pas-de-l-hexadécimal", BODY, SECRET, NOW));
        // Aucune clé configurée : refuser plutôt que laisser passer.
        assertThrows(ForbiddenException.class,
                () -> SignedPayload.requireValid(header(NOW, BODY, SECRET), BODY, "", NOW));
    }

    private static String header(Instant signedAt, String body, String secret) {
        long timestamp = signedAt.getEpochSecond();
        return "t=" + timestamp + ",v1=" + hmac(secret, timestamp + "." + body);
    }

    private static String hmac(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
