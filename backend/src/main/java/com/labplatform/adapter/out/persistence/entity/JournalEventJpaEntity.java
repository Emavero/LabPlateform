package com.labplatform.adapter.out.persistence.entity;

import com.labplatform.domain.journal.JournalKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Une ligne par acte. Table en ajout seul : rien ne s'y modifie. */
@Entity
@Table(name = "journal_event")
public class JournalEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 32)
    private JournalKind kind;

    @Column(name = "subject", length = 128)
    private String subject;

    @Column(name = "detail", length = 255)
    private String detail;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected JournalEventJpaEntity() {
        // requis par JPA
    }

    public JournalEventJpaEntity(Long id, Long userId, JournalKind kind, String subject, String detail,
                                 Instant occurredAt) {
        this.id = id;
        this.userId = userId;
        this.kind = kind;
        this.subject = subject;
        this.detail = detail;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public JournalKind getKind() {
        return kind;
    }

    public String getSubject() {
        return subject;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
