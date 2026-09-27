package com.labplatform.domain.support;

import com.labplatform.domain.shared.InvalidInputException;

import java.time.Instant;
import java.util.Objects;

/**
 * Un message d'une demande d'assistance.
 * <p>
 * Le message retient qui l'a écrit <em>et</em> s'il vient de l'équipe : les
 * deux ne se déduisent pas l'un de l'autre, puisqu'un administrateur peut
 * ouvrir une demande pour lui-même. C'est ce drapeau, et non le rôle actuel de
 * l'auteur, qui décide de quel côté du fil le message s'affiche — un compte
 * promu administrateur ne réécrit donc pas l'histoire des fils déjà écrits.
 */
public class TicketMessage {

    private static final int MAX_BODY_LENGTH = 8_000;

    private final Long id;
    private final Long authorId;
    private final boolean fromStaff;
    private final String body;
    private final Instant sentAt;

    private TicketMessage(Long id, Long authorId, boolean fromStaff, String body, Instant sentAt) {
        this.id = id;
        this.authorId = Objects.requireNonNull(authorId, "authorId");
        this.fromStaff = fromStaff;
        this.body = requireBody(body);
        this.sentAt = Objects.requireNonNull(sentAt, "sentAt");
    }

    static TicketMessage write(Long authorId, boolean fromStaff, String body, Instant sentAt) {
        return new TicketMessage(null, authorId, fromStaff, body, sentAt);
    }

    public static TicketMessage restore(Long id, Long authorId, boolean fromStaff, String body, Instant sentAt) {
        return new TicketMessage(Objects.requireNonNull(id, "id"), authorId, fromStaff, body, sentAt);
    }

    private static String requireBody(String body) {
        if (body == null || body.isBlank()) {
            throw new InvalidInputException("Le message est vide");
        }
        String trimmed = body.strip();
        if (trimmed.length() > MAX_BODY_LENGTH) {
            throw new InvalidInputException("Le message est limité à " + MAX_BODY_LENGTH + " caractères");
        }
        return trimmed;
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
