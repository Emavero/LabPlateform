package com.labplatform.application.port.in.support;

import com.labplatform.domain.support.TicketCategory;
import com.labplatform.domain.user.Actor;

public interface OpenTicketUseCase {

    /** Ouvre une demande au nom de l'appelant, avec son premier message. */
    TicketView open(Actor actor, TicketCategory category, String subject, String body);
}
