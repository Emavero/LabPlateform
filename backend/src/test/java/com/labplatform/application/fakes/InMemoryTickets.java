package com.labplatform.application.fakes;

import com.labplatform.application.port.out.TicketRepositoryPort;
import com.labplatform.domain.support.Ticket;
import com.labplatform.domain.support.TicketMessage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Demandes d'assistance en mémoire.
 * <p>
 * Les messages reçoivent un identifiant à l'enregistrement, comme en base :
 * sans cela, l'adaptateur réel et celui-ci ne se comporteraient pas pareil
 * face à un fil déjà écrit, et le test ne dirait rien de la production.
 */
public class InMemoryTickets implements TicketRepositoryPort {

    private final Map<Long, Ticket> store = new LinkedHashMap<>();
    private long tickets = 0;
    private long messages = 0;

    @Override
    public List<Ticket> findByAuthor(Long authorId) {
        return store.values().stream()
                .filter(ticket -> ticket.getAuthorId().equals(authorId))
                .sorted(Comparator.comparing(Ticket::getUpdatedAt).reversed())
                .toList();
    }

    @Override
    public List<Ticket> findAll() {
        return store.values().stream().sorted(Comparator.comparing(Ticket::getUpdatedAt).reversed()).toList();
    }

    @Override
    public Optional<Ticket> find(Long ticketId) {
        return Optional.ofNullable(store.get(ticketId));
    }

    @Override
    public Ticket save(Ticket ticket) {
        Long id = ticket.getId() != null ? ticket.getId() : ++tickets;
        List<TicketMessage> stored = new ArrayList<>();
        for (TicketMessage message : ticket.getMessages()) {
            stored.add(message.getId() != null ? message
                    : TicketMessage.restore(++messages, message.getAuthorId(), message.isFromStaff(),
                            message.getBody(), message.getSentAt()));
        }
        Ticket saved = Ticket.restore(id, ticket.getAuthorId(), ticket.getCategory(), ticket.getSubject(),
                ticket.getStatus(), ticket.getCreatedAt(), ticket.getUpdatedAt(), stored);
        store.put(id, saved);
        return saved;
    }
}
