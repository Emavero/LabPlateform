package com.labplatform.adapter.out.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Demande d'assistance et son fil.
 * <p>
 * Les messages sont une collection en cascade, et non une entité à part
 * entière : ils n'existent pas hors de leur demande et ne se lisent jamais
 * seuls. Supprimer la demande supprime le fil, ce qui est la seule lecture
 * sensée d'une conversation effacée.
 */
@Entity
@Table(name = "support_ticket")
public class SupportTicketJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 16)
    private com.labplatform.domain.support.TicketCategory category;

    @Column(name = "subject", nullable = false, length = 140)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private com.labplatform.domain.support.TicketStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ticket_id", nullable = false)
    @OrderBy("sentAt ASC, id ASC")
    private List<SupportMessageJpaEntity> messages = new ArrayList<>();

    protected SupportTicketJpaEntity() {
        // requis par JPA
    }

    public SupportTicketJpaEntity(Long id, Long authorId, com.labplatform.domain.support.TicketCategory category,
                                  String subject, com.labplatform.domain.support.TicketStatus status,
                                  Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.authorId = authorId;
        this.category = category;
        this.subject = subject;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void update(com.labplatform.domain.support.TicketStatus status, Instant updatedAt) {
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public com.labplatform.domain.support.TicketCategory getCategory() {
        return category;
    }

    public String getSubject() {
        return subject;
    }

    public com.labplatform.domain.support.TicketStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<SupportMessageJpaEntity> getMessages() {
        return messages;
    }
}
