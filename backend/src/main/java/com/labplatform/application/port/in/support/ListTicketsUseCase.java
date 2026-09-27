package com.labplatform.application.port.in.support;

import com.labplatform.domain.user.Actor;

import java.util.List;

public interface ListTicketsUseCase {

    /** Demandes de l'appelant, les plus récemment remuées d'abord. */
    List<TicketView> listMine(Actor actor);

    /**
     * Demande précise, avec son fil.
     *
     * @throws com.labplatform.domain.shared.NotFoundException demande inexistante
     *                                                        ou qui n'appartient pas à l'appelant
     */
    TicketView get(Actor actor, Long ticketId);
}
