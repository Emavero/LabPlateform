package com.labplatform.domain.user;

import com.labplatform.domain.shared.InvalidInputException;

/**
 * Règles applicables à tout nouveau mot de passe (inscription, changement,
 * réinitialisation). Centralisées ici pour qu'aucun cas d'usage ne les oublie.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 128;

    private PasswordPolicy() {
    }

    public static void validateNewPassword(String password, String confirmation) {
        if (password == null || password.isEmpty()) {
            throw new InvalidInputException("Le mot de passe est obligatoire");
        }
        if (password.length() < MIN_LENGTH) {
            throw new InvalidInputException("Le mot de passe doit contenir au moins " + MIN_LENGTH + " caractères");
        }
        if (password.length() > MAX_LENGTH) {
            throw new InvalidInputException("Le mot de passe ne doit pas dépasser " + MAX_LENGTH + " caractères");
        }
        if (!password.equals(confirmation)) {
            throw new InvalidInputException("Le mot de passe et sa confirmation ne correspondent pas");
        }
    }
}
