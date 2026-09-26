package com.labplatform.domain.academy;

import com.labplatform.domain.shared.InvalidInputException;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Section d'un cours : l'unité que l'apprenant coche une fois terminée.
 *
 * @param position rang dans le cours, à partir de 1
 * @param minutes  durée indicative, qui alimente la durée totale du cours
 */
public record CourseSection(Long id, String slug, String title, SectionKind kind, int position, int minutes,
                            String content) {

    private static final Pattern SLUG_FORMAT = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    public CourseSection {
        if (slug == null || !SLUG_FORMAT.matcher(slug).matches()) {
            throw new InvalidInputException("Identifiant de section invalide");
        }
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(content, "content");
        if (position < 1) {
            throw new IllegalArgumentException("La première section porte le numéro 1");
        }
        if (minutes < 0) {
            throw new IllegalArgumentException("Une durée négative n'a pas de sens : " + minutes);
        }
    }
}
