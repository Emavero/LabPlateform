package com.labplatform.domain.support;

import com.labplatform.domain.shared.InvalidInputException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Demande d'assistance : un sujet, et le fil des messages échangés.
 * <p>
 * La demande est l'agrégat ; ses messages n'existent pas sans elle et ne se
 * modifient jamais. Le statut n'est pas saisi, il se déduit du dernier message :
 * l'équipe répond, la demande attend le demandeur ; le demandeur écrit, elle
 * attend l'équipe. Une file d'attente ne peut donc pas mentir, et personne n'a
 * à penser à la tenir à jour.
 * <p>
 * Une demande résolue accepte encore des messages, et le premier la rouvre :
 * « c'est toujours cassé » est la suite de la même conversation, pas une
 * nouvelle demande, et un fil clos serait une impasse pour qui n'a pas obtenu
 * ce qu'il demandait.
 */
public class Ticket {

    private static final int MAX_SUBJECT_LENGTH = 140;

    private final Long id;
    private final Long authorId;
    private final TicketCategory category;
    private final String subject;
    private TicketStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<TicketMessage> messages;

    private Ticket(Long id, Long authorId, TicketCategory category, String subject, TicketStatus status,
                   Instant createdAt, Instant updatedAt, List<TicketMessage> messages) {
        this.id = id;
        this.authorId = Objects.requireNonNull(authorId, "authorId");
        this.category = Objects.requireNonNull(category, "category");
        this.subject = requireSubject(subject);
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.messages = new ArrayList<>(Objects.requireNonNull(messages, "messages"));
    }

    /** Ouverture : une demande naît toujours avec son premier message. */
    public static Ticket open(Long authorId, TicketCategory category, String subject, String body, Instant now) {
        Ticket ticket = new Ticket(null, authorId, category, subject, TicketStatus.OPEN, now, now, List.of());
        ticket.messages.add(TicketMessage.write(authorId, false, body, now));
        return ticket;
    }

    public static Ticket restore(Long id, Long authorId, TicketCategory category, String subject, TicketStatus status,
                                 Instant createdAt, Instant updatedAt, List<TicketMessage> messages) {
        return new Ticket(Objects.requireNonNull(id, "id"), authorId, category, subject, status, createdAt, updatedAt,
                messages);
    }

    /**
     * Ajoute un message au fil et en tire le statut.
     *
     * @param fromStaff message écrit au nom de l'équipe, et non par le demandeur
     */
    public TicketMessage reply(Long authorId, boolean fromStaff, String body, Instant now) {
        TicketMessage message = TicketMessage.write(authorId, fromStaff, body, now);
        messages.add(message);
        this.status = fromStaff ? TicketStatus.ANSWERED : TicketStatus.OPEN;
        this.updatedAt = Objects.requireNonNull(now, "now");
        return message;
    }

    /**
     * Clôt la demande. Sans effet si elle l'était déjà — deux clics sur le même
     * bouton ne sont pas une erreur à signaler.
     *
     * @return vrai si l'état a changé
     */
    public boolean resolve(Instant now) {
        if (status == TicketStatus.RESOLVED) {
            return false;
        }
        this.status = TicketStatus.RESOLVED;
        this.updatedAt = Objects.requireNonNull(now, "now");
        return true;
    }

    /** Le demandeur lit la sienne ; l'équipe lit toutes les demandes. */
    public boolean isReadableBy(Long userId, boolean staff) {
        return staff || authorId.equals(userId);
    }

    public boolean isOpenedBy(Long userId) {
        return authorId.equals(userId);
    }

    /** Délai de réponse, du premier message à la première réponse de l'équipe. */
    public boolean hasStaffReply() {
        return messages.stream().anyMatch(TicketMessage::isFromStaff);
    }

    private static String requireSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new InvalidInputException("Le sujet est obligatoire");
        }
        String trimmed = subject.strip();
        if (trimmed.length() > MAX_SUBJECT_LENGTH) {
            throw new InvalidInputException("Le sujet est limité à " + MAX_SUBJECT_LENGTH + " caractères");
        }
        return trimmed;
    }

    public Long getId() {
        return id;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public TicketCategory getCategory() {
        return category;
    }

    public String getSubject() {
        return subject;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Le fil, du plus ancien au plus récent. Non modifiable : il ne se réécrit pas. */
    public List<TicketMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public int getMessageCount() {
        return messages.size();
    }
}
