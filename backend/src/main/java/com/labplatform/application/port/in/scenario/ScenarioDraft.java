package com.labplatform.application.port.in.scenario;

import com.labplatform.domain.scenario.ScenarioObjective;
import com.labplatform.domain.scenario.ScenarioStepKind;

import java.util.List;

/**
 * Scénario en cours de saisie dans l'éditeur d'administration.
 *
 * @param steps étapes dans l'ordre de la liste : la position vient de l'ordre,
 *              elle n'est pas saisie — deux étapes ne peuvent donc pas se
 *              disputer la même place
 */
public record ScenarioDraft(String title, String brief, boolean published, List<StepDraft> steps) {

    public record StepDraft(ScenarioStepKind kind, String reference, String instruction, ScenarioObjective objective) {
    }
}
