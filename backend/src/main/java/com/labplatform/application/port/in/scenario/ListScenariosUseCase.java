package com.labplatform.application.port.in.scenario;

import com.labplatform.domain.user.Actor;

import java.util.List;

public interface ListScenariosUseCase {

    /** Scénarios publiés, avec l'avancement de l'appelant. */
    List<ScenarioView> listPublished(Actor actor);

    /**
     * Scénario par son lien.
     *
     * @throws com.labplatform.domain.shared.NotFoundException lien inconnu, ou
     *         brouillon demandé par un compte qui n'est pas administrateur
     */
    ScenarioView get(Actor actor, String slug);
}
