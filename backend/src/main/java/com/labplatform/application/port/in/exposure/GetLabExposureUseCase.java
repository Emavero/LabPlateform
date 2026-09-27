package com.labplatform.application.port.in.exposure;

import com.labplatform.domain.user.Actor;

public interface GetLabExposureUseCase {

    /**
     * Surface d'attaque du lab, notée pour cet appelant.
     * <p>
     * Le résultat dépend de sa formule : les cibles réservées y figurent
     * toujours — leur existence n'est pas un secret — mais sans leur adresse
     * tant qu'il n'est pas abonné, exactement comme sur leur fiche.
     */
    LabExposure exposure(Actor actor);
}
