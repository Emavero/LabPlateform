package com.labplatform.application.port.in.scenario;

import com.labplatform.domain.scenario.Scenario;
import com.labplatform.domain.user.Actor;

import java.util.List;

public interface ManageScenariosUseCase {

    /** Tous les scénarios, brouillons compris. Réservé à l'administration. */
    List<ScenarioView> listAll(Actor actor);

    /** Crée un scénario. Le lien est dérivé du titre. */
    Scenario create(Actor actor, ScenarioDraft draft);

    /** Réécrit un scénario existant. Son lien ne change pas. */
    Scenario update(Actor actor, String slug, ScenarioDraft draft);

    void delete(Actor actor, String slug);
}
