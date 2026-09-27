package com.labplatform.domain.scenario;

import com.labplatform.domain.shared.InvalidInputException;

import java.util.Objects;

/**
 * Étape d'un scénario : une consigne, et la ressource sur laquelle elle porte.
 * <p>
 * La ressource est désignée par son lien ({@code slug}) et non par un
 * identifiant : un scénario reste lisible après la republication d'un cours, et
 * il se relit sans jointure. En contrepartie, renommer le lien d'une machine
 * casse l'étape — c'est pourquoi les liens ne changent pas une fois publiés.
 *
 * @param reference lien de la machine ou du cours visé
 * @param objective ce qui valide l'étape, ignoré pour un cours
 */
public record ScenarioStep(Long id, int position, ScenarioStepKind kind, String reference, String instruction,
                           ScenarioObjective objective) {

    private static final int MAX_INSTRUCTION_LENGTH = 1_000;

    public ScenarioStep {
        Objects.requireNonNull(kind, "kind");
        if (reference == null || reference.isBlank()) {
            throw new InvalidInputException("Chaque étape désigne une machine ou un cours");
        }
        if (instruction != null && instruction.length() > MAX_INSTRUCTION_LENGTH) {
            throw new InvalidInputException("La consigne est limitée à " + MAX_INSTRUCTION_LENGTH + " caractères");
        }
        reference = reference.trim();
        instruction = instruction == null || instruction.isBlank() ? null : instruction.strip();
        // Un cours se termine ou non : lui prêter un objectif de flag laisserait
        // croire à un choix qui n'existe pas.
        objective = kind == ScenarioStepKind.MACHINE
                ? (objective == null ? ScenarioObjective.USER_FLAG : objective)
                : null;
    }

    /** Étape telle que la saisit l'éditeur : sans identifiant, position imposée. */
    public static ScenarioStep drafted(int position, ScenarioStepKind kind, String reference, String instruction,
                                       ScenarioObjective objective) {
        return new ScenarioStep(null, position, kind, reference, instruction, objective);
    }
}
