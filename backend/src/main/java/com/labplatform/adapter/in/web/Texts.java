package com.labplatform.adapter.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Traduction des textes que le serveur envoie : messages d'erreur et libellés
 * d'affichage.
 * <p>
 * Le français sert de clé, plutôt qu'un code. C'est un choix, et il a un motif :
 * le domaine écrit ses messages en français, là où ils ont un sens, et leur
 * attribuer un code aurait demandé de toucher une centaine d'endroits sans rien
 * apprendre de plus. Un texte absent du catalogue reste en français plutôt que
 * de disparaître, et un test refuse toute entrée du catalogue qui ne correspond
 * plus à aucun texte du code — c'est ce qui empêche la traduction de dériver en
 * silence.
 * <p>
 * L'accès est statique parce que les convertisseurs de DTO sont statiques et
 * qu'ils sont les premiers concernés. La langue vient de
 * {@link LocaleContextHolder}, que Spring alimente depuis l'en-tête
 * {@code Accept-Language} de la requête en cours : c'est le même mécanisme que
 * celui de {@code MessageSource}, sans en imposer le format.
 */
public final class Texts {

    private static final Logger log = LoggerFactory.getLogger(Texts.class);
    private static final Catalogue ENGLISH = Catalogue.load("i18n/en.json");

    private Texts() {
    }

    /** Texte tel que l'appelant de la requête en cours doit le lire. */
    public static String of(String french) {
        return translate(french, LocaleContextHolder.getLocale());
    }

    /** Traduction explicite, sans dépendre de la requête en cours : pour les tests. */
    public static String translate(String french, Locale locale) {
        if (french == null || french.isBlank() || locale == null) {
            return french;
        }
        return "en".equals(locale.getLanguage()) ? ENGLISH.translate(french) : french;
    }

    /** Ce que le catalogue couvre, pour le test qui traque les entrées mortes. */
    public static java.util.Set<String> knownFrenchTexts() {
        return ENGLISH.exact.keySet();
    }

    public static List<String> knownPatterns() {
        return ENGLISH.patterns.stream().map(rule -> rule.match.pattern()).toList();
    }

    /**
     * Le catalogue, chargé une fois. Deux formes s'y côtoient : des textes
     * entiers, et des motifs pour les messages construits par concaténation, où
     * un nombre ou un nom varie — « Le titre est limité à 128 caractères ».
     */
    private static final class Catalogue {

        private final Map<String, String> exact;
        private final List<Rule> patterns;

        private Catalogue(Map<String, String> exact, List<Rule> patterns) {
            this.exact = exact;
            this.patterns = patterns;
        }

        static Catalogue load(String resource) {
            try (InputStream stream = new ClassPathResource(resource).getInputStream()) {
                JsonNode root = new ObjectMapper().readTree(stream);
                Map<String, String> exact = new LinkedHashMap<>();
                root.path("exact").fields()
                        .forEachRemaining(entry -> exact.put(entry.getKey(), entry.getValue().asText()));
                List<Rule> patterns = new ArrayList<>();
                for (JsonNode rule : root.path("patterns")) {
                    patterns.add(new Rule(Pattern.compile(rule.get(0).asText()), rule.get(1).asText()));
                }
                return new Catalogue(Map.copyOf(exact), List.copyOf(patterns));
            } catch (IOException | RuntimeException unreadable) {
                // Un catalogue illisible ne doit pas empêcher la plateforme de
                // démarrer : elle reste en français, ce qui est lisible.
                log.error("Catalogue de traduction {} illisible : les textes resteront en français", resource,
                        unreadable);
                return new Catalogue(Map.of(), List.of());
            }
        }

        String translate(String french) {
            String exactMatch = exact.get(french);
            if (exactMatch != null) {
                return exactMatch;
            }
            for (Rule rule : patterns) {
                Matcher matcher = rule.match.matcher(french);
                if (matcher.matches()) {
                    return matcher.replaceFirst(rule.replacement);
                }
            }
            // Rien ne correspond : le français, qui se lit, plutôt qu'un vide.
            return french;
        }

        private record Rule(Pattern match, String replacement) {
        }
    }
}
