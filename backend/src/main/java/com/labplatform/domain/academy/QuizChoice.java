package com.labplatform.domain.academy;

import com.labplatform.domain.shared.InvalidInputException;

/**
 * Proposition de réponse. Le drapeau « correcte » ne quitte jamais le
 * serveur avant la correction : il n'apparaît ni dans la fiche du cours, ni
 * dans l'énoncé servi à l'apprenant.
 */
public record QuizChoice(Long id, String label, boolean correct, int position) {

    private static final int MAX_LABEL_LENGTH = 256;

    public QuizChoice {
        if (label == null || label.isBlank()) {
            throw new InvalidInputException("Une proposition ne peut pas être vide");
        }
        label = label.trim();
        if (label.length() > MAX_LABEL_LENGTH) {
            throw new InvalidInputException("Une proposition est limitée à " + MAX_LABEL_LENGTH + " caractères");
        }
        if (position < 1) {
            throw new IllegalArgumentException("La première proposition porte le numéro 1");
        }
    }
}
