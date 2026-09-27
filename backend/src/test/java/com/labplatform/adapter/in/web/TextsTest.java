package com.labplatform.adapter.in.web;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le catalogue de traduction a une faiblesse connue : il est indexé par le
 * texte français, donc reformuler un message dans le code le décroche
 * silencieusement de sa traduction. Ce test est le garde-fou : il relit les
 * sources et refuse toute entrée du catalogue qui ne correspond plus à aucun
 * texte du code.
 */
class TextsTest {

    private static final Locale ENGLISH = Locale.ENGLISH;
    private static final Locale FRENCH = Locale.FRENCH;
    private static final Path SOURCES = Path.of("src/main/java/com/labplatform");

    @Test
    void translatesAKnownMessage() {
        assertEquals("Wrong flag", Texts.translate("Flag incorrect", ENGLISH));
        assertEquals("Machine not found", Texts.translate("Machine introuvable", ENGLISH));
    }

    @Test
    void leavesFrenchAloneForAFrenchSpeaker() {
        assertEquals("Flag incorrect", Texts.translate("Flag incorrect", FRENCH));
        assertEquals("Flag incorrect", Texts.translate("Flag incorrect", null));
    }

    /** Les messages construits par concaténation passent par un motif. */
    @Test
    void translatesAMessageWhoseNumberVaries() {
        assertEquals("The title is limited to 128 characters",
                Texts.translate("Le titre est limité à 128 caractères", ENGLISH));
        assertEquals("The password must be at least 12 characters long",
                Texts.translate("Le mot de passe doit contenir au moins 12 caractères", ENGLISH));
        assertEquals("Cannot add EUR and XOF together",
                Texts.translate("Impossible d'additionner des EUR et des XOF", ENGLISH));
    }

    @Test
    void translatesDisplayLabelsAsWellAsErrors() {
        assertEquals("Very easy", Texts.translate("Très facile", ENGLISH));
        assertEquals("Machine hunter", Texts.translate("Chasseur de machines", ENGLISH));
        assertEquals("Flag accepted", Texts.translate("Flag validé", ENGLISH));
    }

    /** Un texte inconnu reste lisible : le français, jamais rien. */
    @Test
    void leavesAnUnknownTextUntouched() {
        assertEquals("Un message que personne n'a traduit",
                Texts.translate("Un message que personne n'a traduit", ENGLISH));
        assertEquals("", Texts.translate("", ENGLISH));
        assertEquals(null, Texts.translate(null, ENGLISH));
    }

    /**
     * Le garde-fou : chaque texte du catalogue doit encore exister dans le code.
     * Une entrée orpheline signifie qu'un message a été reformulé et que sa
     * traduction ne s'applique plus — le genre de dérive qu'on ne voit pas.
     */
    @Test
    void everyCatalogueEntryStillMatchesATextInTheCode() {
        String code = readAllSources();
        List<String> orphans = Texts.knownFrenchTexts().stream()
                .filter(french -> !code.contains(escapeForJava(french)))
                .sorted()
                .toList();

        assertTrue(orphans.isEmpty(),
                "Ces textes du catalogue n'existent plus dans le code : " + orphans
                        + ". Reformulé un message ? Mets à jour i18n/en.json.");
    }

    /** Même garde-fou pour les motifs : un motif qui ne filtre plus rien est mort. */
    @Test
    void everyPatternStillMatchesAMessageShapeInTheCode() {
        String code = readAllSources();
        List<String> unused = Texts.knownPatterns().stream()
                .filter(pattern -> !matchesSomething(pattern, code))
                .sorted()
                .toList();

        assertTrue(unused.isEmpty(), "Ces motifs de traduction ne correspondent plus à rien : " + unused);
    }

    /**
     * Un motif est considéré utilisé si son début littéral — la partie avant la
     * première capture — se retrouve dans le code. C'est volontairement souple :
     * on cherche une dérive de formulation, pas à rejouer la concaténation.
     */
    private static boolean matchesSomething(String pattern, String code) {
        Matcher capture = Pattern.compile("\\(").matcher(pattern);
        int firstGroup = capture.find() ? capture.start() : pattern.length();
        String literal = pattern.substring(1, Math.max(firstGroup, 1))
                .replace("\\.", ".")
                .replace("\\d", "")
                .trim();
        return literal.length() < 4 || code.contains(escapeForJava(literal));
    }

    /** Les sources écrivent les guillemets échappés : le catalogue, non. */
    private static String escapeForJava(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String readAllSources() {
        try (Stream<Path> files = Files.walk(SOURCES)) {
            StringBuilder all = new StringBuilder();
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                all.append(Files.readString(file)).append('\n');
            }
            return all.toString();
        } catch (IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    /** Le catalogue ne doit pas laisser un texte se traduire par lui-même par oubli. */
    @Test
    void doesNotContainEmptyTranslations() {
        Set<String> french = Texts.knownFrenchTexts();

        assertTrue(french.stream().noneMatch(String::isBlank), "une clé vide s'est glissée dans le catalogue");
        assertTrue(french.size() > 100, "le catalogue devrait couvrir l'essentiel des textes du serveur");
    }
}
