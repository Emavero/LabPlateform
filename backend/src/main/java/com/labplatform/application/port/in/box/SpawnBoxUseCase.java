package com.labplatform.application.port.in.box;

import com.labplatform.domain.user.Actor;

public interface SpawnBoxUseCase {

    /**
     * Lance la cible d'une machine pour l'appelant et renvoie la fiche à jour.
     *
     * @throws com.labplatform.domain.shared.ConflictException           une autre cible tourne déjà
     * @throws com.labplatform.domain.shared.ServiceUnavailableException l'infrastructure n'a pas pu la lancer
     */
    BoxView spawn(Actor actor, String slug);

    /** Arrête la cible. Sans effet si elle ne tournait pas. */
    BoxView stop(Actor actor, String slug);
}
