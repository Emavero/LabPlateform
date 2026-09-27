package com.labplatform.application.port.in.scenario;

import com.labplatform.domain.scenario.Scenario;
import com.labplatform.domain.scenario.ScenarioProgress;

import java.util.List;

/**
 * Scénario tel qu'il est présenté à un joueur.
 *
 * @param steps    étapes enrichies du nom de la ressource visée : un lien
 *                 ({@code slug}) ne se lit pas, un nom se lit
 * @param progress où en est le lecteur
 */
public record ScenarioView(Scenario scenario, List<ScenarioStepView> steps, ScenarioProgress progress) {

    /**
     * Étape présentée.
     *
     * @param name    nom de la machine ou du cours, ou le lien si la ressource a
     *                disparu du catalogue
     * @param missing la ressource n'existe plus : l'étape est infranchissable et
     *                l'administration doit le voir
     * @param locked  machine réservée aux abonnés : l'étape reste visible, son
     *                détail non
     * @param done    étape franchie par le lecteur
     */
    public record ScenarioStepView(com.labplatform.domain.scenario.ScenarioStep step, String name, boolean missing,
                                   boolean locked, boolean done) {
    }
}
