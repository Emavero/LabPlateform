package com.labplatform.adapter.out.persistence;

import com.labplatform.adapter.out.persistence.entity.SupportMessageJpaEntity;
import com.labplatform.adapter.out.persistence.entity.SupportTicketJpaEntity;
import com.labplatform.adapter.out.persistence.repository.SpringDataTicketRepository;
import com.labplatform.application.port.out.TicketRepositoryPort;
import com.labplatform.domain.support.Ticket;
import com.labplatform.domain.support.TicketMessage;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class TicketPersistenceAdapter implements TicketRepositoryPort {

    private final SpringDataTicketRepository repository;

    public TicketPersistenceAdapter(SpringDataTicketRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findByAuthor(Long authorId) {
        return repository.findByAuthorIdOrderByUpdatedAtDesc(authorId).stream()
                .map(TicketPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findAll() {
        return repository.findAllByOrderByUpdatedAtDesc().stream().map(TicketPersistenceAdapter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Ticket> find(Long ticketId) {
        return repository.findWithMessagesById(ticketId).map(TicketPersistenceAdapter::toDomain);
    }

    /**
     * L'enregistrement rejoue le fil du domaine sur la ligne stockée : les
     * messages déjà écrits portent un identifiant et sont laissés tels quels,
     * les nouveaux n'en ont pas encore et sont ajoutés. Un message ne se
     * modifiant jamais, comparer les identifiants suffit — et cela évite de
     * réécrire un fil entier à chaque réponse.
     */
    @Override
    @Transactional
    public Ticket save(Ticket ticket) {
        SupportTicketJpaEntity entity = ticket.getId() == null
                ? new SupportTicketJpaEntity(null, ticket.getAuthorId(), ticket.getCategory(), ticket.getSubject(),
                        ticket.getStatus(), ticket.getCreatedAt(), ticket.getUpdatedAt())
                : repository.findWithMessagesById(ticket.getId()).orElseThrow();
        entity.update(ticket.getStatus(), ticket.getUpdatedAt());
        for (TicketMessage message : ticket.getMessages()) {
            if (message.getId() == null) {
                entity.getMessages().add(new SupportMessageJpaEntity(null, message.getAuthorId(),
                        message.isFromStaff(), message.getBody(), message.getSentAt()));
            }
        }
        return toDomain(repository.save(entity));
    }

    private static Ticket toDomain(SupportTicketJpaEntity e) {
        List<TicketMessage> messages = e.getMessages().stream()
                .map(m -> TicketMessage.restore(m.getId(), m.getAuthorId(), m.isFromStaff(), m.getBody(),
                        m.getSentAt()))
                .toList();
        return Ticket.restore(e.getId(), e.getAuthorId(), e.getCategory(), e.getSubject(), e.getStatus(),
                e.getCreatedAt(), e.getUpdatedAt(), messages);
    }
}
