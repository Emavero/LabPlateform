package com.labplatform.domain.academy;

import com.labplatform.domain.shared.InvalidInputException;

/**
 * Étape d'une chaîne d'attaque : ce que fait l'attaquant, et par quel moyen.
 * <p>
 * La technique est facultative — toutes les étapes ne portent pas de référence
 * connue — mais le nom et la description ne le sont pas : une étape sans texte
 * n'illustre rien.
 */
public record AttackStage(Long id, int position, String name, String description, String technique) {

    public static AttackStage of(Long id, int position, String name, String description, String technique) {
        if (position < 1) {
            throw new InvalidInputException("Position d'étape invalide");
        }
        return new AttackStage(id, position, require(name, "Nom d'étape manquant"),
                require(description, "Description d'étape manquante"), blankToNull(technique));
    }

    private static String require(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidInputException(message);
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
