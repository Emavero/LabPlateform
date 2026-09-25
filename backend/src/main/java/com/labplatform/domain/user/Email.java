package com.labplatform.domain.user;

import com.labplatform.domain.shared.InvalidInputException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Objet-valeur : une adresse e-mail est toujours normalisée (trim + minuscules)
 * et valide dès sa construction. "Alice@x.io" et "alice@x.io" désignent donc
 * le même compte.
 */
public record Email(String value) {

    private static final int MAX_LENGTH = 254;
    private static final Pattern FORMAT = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

    public Email {
        if (value == null || value.isBlank()) {
            throw new InvalidInputException("L'adresse e-mail est obligatoire");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches()) {
            throw new InvalidInputException("Le format de l'adresse e-mail est invalide");
        }
    }

    public static Email of(String raw) {
        return new Email(raw);
    }

    @Override
    public String toString() {
        return value;
    }
}
