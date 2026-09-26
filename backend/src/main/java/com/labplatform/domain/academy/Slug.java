package com.labplatform.domain.academy;

import com.labplatform.domain.shared.InvalidInputException;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/**
 * Fabrique d'identifiants d'URL à partir d'un titre saisi : accents retirés,
 * minuscules, tirets. « Réponse à incident » donne « reponse-a-incident ».
 */
public final class Slug {

    private static final int MAX_LENGTH = 64;

    private Slug() {
    }

    public static String from(String title) {
        if (title == null || title.isBlank()) {
            throw new InvalidInputException("Le titre est obligatoire");
        }
        String slug = Normalizer.normalize(title.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        if (slug.isEmpty()) {
            throw new InvalidInputException("Le titre doit contenir des lettres ou des chiffres");
        }
        return slug.length() > MAX_LENGTH ? trimTrailingDash(slug.substring(0, MAX_LENGTH)) : slug;
    }

    /** Variante non prise parmi celles déjà utilisées : « intro », « intro-2 »… */
    public static String uniqueFrom(String title, Set<String> taken) {
        String base = from(title);
        if (!taken.contains(base)) {
            return base;
        }
        for (int suffix = 2; suffix < 1_000; suffix++) {
            String candidate = trimTrailingDash(truncate(base, suffix)) + "-" + suffix;
            if (!taken.contains(candidate)) {
                return candidate;
            }
        }
        throw new InvalidInputException("Trop de sections portent ce titre");
    }

    private static String truncate(String base, int suffix) {
        int room = MAX_LENGTH - String.valueOf(suffix).length() - 1;
        return base.length() > room ? base.substring(0, room) : base;
    }

    private static String trimTrailingDash(String value) {
        return value.replaceAll("-+$", "");
    }
}
