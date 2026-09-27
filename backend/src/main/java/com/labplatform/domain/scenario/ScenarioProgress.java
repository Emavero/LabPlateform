package com.labplatform.domain.scenario;

import java.util.List;

/**
 * Avancement d'un joueur sur un scénario.
 *
 * @param nextPosition position de la première étape non franchie, ou null quand
 *                     le scénario est terminé
 * @param doneStepIds  étapes franchies, pour les cocher dans la liste
 */
public record ScenarioProgress(int done, int total, Integer nextPosition, List<Long> doneStepIds) {

    public boolean isComplete() {
        return total > 0 && done == total;
    }

    /** Part franchie, de 0 à 1. Un scénario sans étape vaut zéro, jamais « terminé ». */
    public double ratio() {
        return total == 0 ? 0 : (double) done / total;
    }
}
