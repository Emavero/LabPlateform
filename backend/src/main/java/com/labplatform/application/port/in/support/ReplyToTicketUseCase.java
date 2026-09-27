package com.labplatform.application.port.in.support;

import com.labplatform.domain.user.Actor;

public interface ReplyToTicketUseCase {

    /**
     * Ajoute un message au fil. Le message est marqué comme venant de l'équipe
     * quand c'est un administrateur qui écrit dans la demande d'un autre.
     */
    TicketView reply(Actor actor, Long ticketId, String body);

    /** Marque la demande comme résolue. Le demandeur comme l'équipe peuvent le faire. */
    TicketView resolve(Actor actor, Long ticketId);
}
