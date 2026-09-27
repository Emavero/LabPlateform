package com.labplatform.domain.scenario;

/**
 * Ce qui valide une étape sur une machine.
 * <p>
 * L'entrée et l'élévation sont distinguées parce qu'un scénario d'entraînement
 * s'arrête souvent à l'une : « obtenir un pied dans le système » est un
 * objectif en soi, et exiger root partout rendrait la plupart des scénarios
 * infranchissables pour un débutant.
 */
public enum ScenarioObjective {

    USER_FLAG("Flag utilisateur"),
    ROOT_FLAG("Flag root"),
    BOTH_FLAGS("Les deux flags");

    private final String displayName;

    ScenarioObjective(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
