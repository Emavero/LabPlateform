package com.labplatform.domain.scoring;

import java.util.Locale;

/**
 * Pseudonyme affiché aux autres joueurs. Le classement est public : il ne
 * doit pas y exposer d'adresse e-mail complète, donc seule la partie locale
 * est retenue, tronquée et nettoyée.
 */
public final class Handle {

    private static final int MAX_LENGTH = 20;

    private Handle() {
    }

    public static String fromEmail(String email) {
        if (email == null || email.isBlank()) {
            return "anonyme";
        }
        String localPart = email.split("@", 2)[0].toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "");
        if (localPart.isBlank()) {
            return "anonyme";
        }
        return localPart.length() > MAX_LENGTH ? localPart.substring(0, MAX_LENGTH) : localPart;
    }
}
