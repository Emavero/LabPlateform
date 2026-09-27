package com.labplatform.application.port.out;

import com.labplatform.domain.support.Ticket;

import java.util.List;
import java.util.Optional;

public interface TicketRepositoryPort {

    /** Demandes d'un compte, les plus récemment remuées d'abord. */
    List<Ticket> findByAuthor(Long authorId);

    /** Toutes les demandes, les plus récemment remuées d'abord : la file de l'équipe. */
    List<Ticket> findAll();

    /** Demande et son fil complet. */
    Optional<Ticket> find(Long ticketId);

    Ticket save(Ticket ticket);
}
