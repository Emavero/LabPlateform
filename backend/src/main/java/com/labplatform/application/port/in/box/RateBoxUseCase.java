package com.labplatform.application.port.in.box;

import com.labplatform.domain.box.Difficulty;
import com.labplatform.domain.user.Actor;

public interface RateBoxUseCase {

    /**
     * Note la difficulté ressentie d'une machine. Voter à nouveau remplace le
     * vote précédent.
     *
     * @throws com.labplatform.domain.shared.ConflictException machine pas encore possédée
     */
    BoxView rateBox(Actor actor, String slug, Difficulty difficulty);
}
