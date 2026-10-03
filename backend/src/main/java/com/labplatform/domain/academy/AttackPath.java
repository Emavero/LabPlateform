package com.labplatform.domain.academy;

import com.labplatform.domain.shared.InvalidInputException;

import java.util.Comparator;
import java.util.List;

/**
 * Chemin d'attaque d'un cours : le vecteur de menace, puis la suite d'étapes
 * qui mène l'attaquant de l'extérieur à son objectif.
 * <p>
 * Un chemin est soit absent, soit complet : un résumé sans étapes n'illustre
 * pas de chaîne, et des étapes sans résumé ne disent pas de quoi elles sont la
 * chaîne. L'absence est une valeur, pas un null — tous les cours n'ont pas
 * encore leur chemin décrit, et la page doit pouvoir s'en passer.
 */
public record AttackPath(String summary, List<AttackStage> stages) {

    private static final AttackPath NONE = new AttackPath(null, List.of());

    public AttackPath {
        stages = stages == null ? List.of() : stages.stream()
                .sorted(Comparator.comparingInt(AttackStage::position))
                .toList();
        summary = summary == null || summary.isBlank() ? null : summary.trim();
        if (summary == null && !stages.isEmpty()) {
            throw new InvalidInputException("Un chemin d'attaque décrit ce que ses étapes enchaînent");
        }
        if (summary != null && stages.isEmpty()) {
            throw new InvalidInputException("Un chemin d'attaque comporte au moins une étape");
        }
    }

    public static AttackPath none() {
        return NONE;
    }

    public static AttackPath of(String summary, List<AttackStage> stages) {
        return new AttackPath(summary, stages);
    }

    public boolean isPresent() {
        return summary != null;
    }
}
