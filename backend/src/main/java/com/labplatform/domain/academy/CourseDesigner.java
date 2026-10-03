package com.labplatform.domain.academy;

import com.labplatform.domain.shared.InvalidInputException;

/**
 * Concepteur d'un scénario de cours : qui l'a écrit, et à quel titre.
 * <p>
 * L'avatar est facultatif — la page sait présenter quelqu'un sans sa photo, par
 * ses initiales — mais le nom et le rôle ne le sont pas : signer un contenu
 * sans dire d'où l'on parle n'apporte rien à celui qui l'étudie.
 */
public record CourseDesigner(Long id, int position, String name, String role, String avatarUrl) {

    public static CourseDesigner of(Long id, int position, String name, String role, String avatarUrl) {
        if (position < 1) {
            throw new InvalidInputException("Position de concepteur invalide");
        }
        return new CourseDesigner(id, position, require(name, "Nom du concepteur manquant"),
                require(role, "Rôle du concepteur manquant"), blankToNull(avatarUrl));
    }

    /** Initiales, quand il n'y a pas d'avatar à montrer. */
    public String initials() {
        String[] words = name.split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String word : words) {
            if (!word.isBlank() && initials.length() < 2) {
                initials.append(Character.toUpperCase(word.charAt(0)));
            }
        }
        return initials.toString();
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
