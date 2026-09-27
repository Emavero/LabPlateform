package com.labplatform.adapter.out.persistence.repository;

import com.labplatform.adapter.out.persistence.entity.SupportTicketJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataTicketRepository extends JpaRepository<SupportTicketJpaEntity, Long> {

    /**
     * Le fil est chargé avec la demande : la liste d'assistance affiche le
     * dernier message de chaque demande, et le charger demande par demande
     * ferait une requête par ligne.
     */
    @EntityGraph(attributePaths = "messages")
    List<SupportTicketJpaEntity> findByAuthorIdOrderByUpdatedAtDesc(Long authorId);

    @EntityGraph(attributePaths = "messages")
    List<SupportTicketJpaEntity> findAllByOrderByUpdatedAtDesc();

    @EntityGraph(attributePaths = "messages")
    Optional<SupportTicketJpaEntity> findWithMessagesById(Long id);
}
