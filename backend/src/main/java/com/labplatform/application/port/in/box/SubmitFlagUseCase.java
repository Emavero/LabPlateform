package com.labplatform.application.port.in.box;

import com.labplatform.domain.box.FlagKind;
import com.labplatform.domain.user.Actor;

public interface SubmitFlagUseCase {

    /**
     * Valide un flag pour l'appelant.
     *
     * @throws com.labplatform.domain.shared.NotFoundException     machine inconnue
     * @throws com.labplatform.domain.shared.ConflictException     flag déjà validé par ce joueur
     * @throws com.labplatform.domain.shared.InvalidInputException flag mal formé ou incorrect
     */
    FlagSubmissionResult submitFlag(Actor actor, String slug, FlagKind kind, String flag);
}
