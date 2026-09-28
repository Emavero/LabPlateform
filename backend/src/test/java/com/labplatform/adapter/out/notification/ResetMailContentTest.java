package com.labplatform.adapter.out.notification;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResetMailContentTest {

    private static final Instant NOW = Instant.parse("2026-09-28T10:00:00Z");
    private static final Instant IN_15_MIN = NOW.plus(Duration.ofMinutes(15));
    private static final String TOKEN = "jeton-de-reinitialisation";

    private static ResetMailContent content(String publicUrl, Locale locale) {
        return ResetMailContent.of(publicUrl, TOKEN, IN_15_MIN, NOW, locale);
    }

    @Test
    void leLienMeneALaPagePubliqueAvecLeJeton() {
        String body = content("https://lab.example.org", Locale.FRENCH).body();

        assertTrue(body.contains("https://lab.example.org/reset-password?token=" + TOKEN), body);
    }

    /** Une adresse publique saisie avec une barre finale ne doit pas la doubler. */
    @Test
    void laBarreFinaleDeLAdresseNEstPasDoublee() {
        assertEquals("https://lab.example.org/reset-password?token=abc",
                ResetMailContent.link("https://lab.example.org/", "abc"));
        assertEquals("https://lab.example.org/reset-password?token=abc",
                ResetMailContent.link("https://lab.example.org///", "abc"));
        assertEquals("https://lab.example.org/reset-password?token=abc",
                ResetMailContent.link("  https://lab.example.org  ", "abc"));
    }

    /** Un jeton voyage dans une URL : ce qui s'y lit mal doit être encodé. */
    @Test
    void leJetonEstEncodePourLURL() {
        assertTrue(ResetMailContent.link("https://lab.example.org", "a+b/c=d").endsWith("token=a%2Bb%2Fc%3Dd"));
    }

    @Test
    void leDelaiEstExprimeEnMinutesRestantes() {
        assertTrue(content("https://lab.example.org", Locale.FRENCH).body().contains("15 minutes"));
    }

    /**
     * Le jeton naît quelques centièmes de seconde avant l'envoi : sans arrondi,
     * un lien d'un quart d'heure s'annoncerait « 14 minutes ».
     */
    @Test
    void lesQuelquesMillisecondesDeRetardNeRognentPasUneMinute() {
        ResetMailContent content = ResetMailContent.of("https://lab.example.org", TOKEN, IN_15_MIN,
                NOW.plusMillis(400), Locale.FRENCH);

        assertTrue(content.body().contains("15 minutes"), content.body());
    }

    /** Un lien déjà expiré n'annonce pas « 0 minute » : il reste une minute à afficher. */
    @Test
    void unDelaiEcouleNAnnonceJamaisZeroMinute() {
        ResetMailContent expired = ResetMailContent.of("https://lab.example.org", TOKEN, NOW, NOW, Locale.FRENCH);

        assertTrue(expired.body().contains("1 minutes"), expired.body());
    }

    @Test
    void leMessageEstEnFrancaisParDefaut() {
        ResetMailContent fr = content("https://lab.example.org", Locale.FRENCH);

        assertTrue(fr.subject().contains("réinitialisation"));
        assertTrue(fr.body().contains("Bonjour"));
        assertTrue(fr.body().contains("ignorez ce message"));
    }

    @Test
    void lAnglaisEstServiQuandIlEstDemande() {
        ResetMailContent en = content("https://lab.example.org", Locale.ENGLISH);

        assertTrue(en.subject().contains("reset your password"));
        assertTrue(en.body().contains("Hello"));
        assertTrue(en.body().contains("ignore this message"));
    }

    /** Toute autre langue retombe sur la langue de référence. */
    @Test
    void uneLangueInconnueRetombeSurLeFrancais() {
        assertTrue(content("https://lab.example.org", Locale.GERMAN).body().contains("Bonjour"));
        assertTrue(content("https://lab.example.org", null).body().contains("Bonjour"));
    }

    /** Le message dit ce qu'il faut faire si la demande ne vient pas de soi. */
    @Test
    void leMessageNeContientNiMotDePasseNiIdentifiant() {
        String body = content("https://lab.example.org", Locale.FRENCH).body();

        assertFalse(body.toLowerCase(Locale.ROOT).contains("mot de passe :"));
        assertTrue(body.contains("votre mot de passe reste inchangé"));
    }
}
