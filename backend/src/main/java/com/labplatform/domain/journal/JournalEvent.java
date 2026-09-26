package com.labplatform.domain.journal;

import com.labplatform.domain.shared.InvalidInputException;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Un acte inscrit au journal : qui, quoi, sur quoi, quand.
 * <p>
 * Le journal est en écriture seule : un événement ne se corrige pas, il s'en
 * ajoute un autre. C'est ce qui permet de s'y fier pour compter.
 * <p>
 * {@code subject} désigne la ressource concernée par son lien (le {@code slug}
 * d'une machine ou d'un cours) et non par son identifiant : le journal reste
 * lisible après la suppression de la ressource, et une machine retirée
 * n'efface pas l'histoire de ceux qui l'ont faite.
 */
public class JournalEvent {

    private static final int MAX_SUBJECT_LENGTH = 128;
    private static final int MAX_DETAIL_LENGTH = 255;

    private final Long id;
    private final Long userId;
    private final JournalKind kind;
    private final String subject;
    private final String detail;
    private final Instant occurredAt;

    private JournalEvent(Long id, Long userId, JournalKind kind, String subject, String detail, Instant occurredAt) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId, "userId");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.subject = bounded(subject, MAX_SUBJECT_LENGTH, "subject");
        this.detail = bounded(detail, MAX_DETAIL_LENGTH, "detail");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
    }

    public static JournalEvent of(Long userId, JournalKind kind, String subject, Instant occurredAt) {
        return new JournalEvent(null, userId, kind, subject, null, occurredAt);
    }

    public static JournalEvent of(Long userId, JournalKind kind, String subject, String detail, Instant occurredAt) {
        return new JournalEvent(null, userId, kind, subject, detail, occurredAt);
    }

    public static JournalEvent restore(Long id, Long userId, JournalKind kind, String subject, String detail,
                                       Instant occurredAt) {
        return new JournalEvent(Objects.requireNonNull(id, "id"), userId, kind, subject, detail, occurredAt);
    }

    public JournalFamily family() {
        return kind.family();
    }

    private static String bounded(String value, int max, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new InvalidInputException("Le champ " + field + " du journal est limité à " + max + " caractères");
        }
        return trimmed;
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

    public Optional<String> getSubject() {
        return Optional.ofNullable(subject);
    }

    public Optional<String> getDetail() {
        return Optional.ofNullable(detail);
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
