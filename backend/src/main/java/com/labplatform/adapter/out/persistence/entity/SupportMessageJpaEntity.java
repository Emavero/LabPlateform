package com.labplatform.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Message d'une demande d'assistance. Écrit une fois, jamais modifié. */
@Entity
@Table(name = "support_message")
public class SupportMessageJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "from_staff", nullable = false)
    private boolean fromStaff;

    /* Pas de @Lob : sur PostgreSQL il écrirait un « large object » (voir docs/ARCHITECTURE.md). */
    @Column(name = "body", nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    protected SupportMessageJpaEntity() {
        // requis par JPA
    }

    public SupportMessageJpaEntity(Long id, Long authorId, boolean fromStaff, String body, Instant sentAt) {
        this.id = id;
        this.authorId = authorId;
        this.fromStaff = fromStaff;
        this.body = body;
        this.sentAt = sentAt;
    }

    public Long getId() {
        return id;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public boolean isFromStaff() {
        return fromStaff;
    }

    public String getBody() {
        return body;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
