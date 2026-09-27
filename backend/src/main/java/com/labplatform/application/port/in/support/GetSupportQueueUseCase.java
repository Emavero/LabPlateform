package com.labplatform.application.port.in.support;

import com.labplatform.domain.user.Actor;

public interface GetSupportQueueUseCase {

    /** Toutes les demandes, classées par ce qu'elles attendent. Réservé à l'équipe. */
    SupportQueue queue(Actor actor);
}
