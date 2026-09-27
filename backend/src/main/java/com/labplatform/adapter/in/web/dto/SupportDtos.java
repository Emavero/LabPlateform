package com.labplatform.adapter.in.web.dto;

import com.labplatform.adapter.in.web.Texts;
import com.labplatform.application.port.in.support.SupportQueue;
import com.labplatform.application.port.in.support.TicketView;
import com.labplatform.domain.support.Ticket;
import com.labplatform.domain.support.TicketMessage;

import java.time.Instant;
import java.util.List;

/**
 * Formes exposées par l'assistance.
 * <p>
 * Le fil n'est envoyé qu'avec la demande demandée : la liste porte le nombre de
 * messages et le dernier extrait, pas la conversation entière — inutile de
 * transporter des centaines de messages pour afficher dix lignes.
 */
public final class SupportDtos {

    private SupportDtos() {
    }

    public record MessageResponse(boolean fromStaff, String body, Instant sentAt) {

        static MessageResponse from(TicketMessage message) {
            return new MessageResponse(message.isFromStaff(), message.getBody(), message.getSentAt());
        }
    }

    /** Ligne de liste : ce qu'il faut pour choisir quelle demande ouvrir. */
    public record TicketSummaryResponse(Long id, String handle, boolean mine, String category, String categoryName,
                                        String subject, String status, String statusName, int messages,
                                        String lastMessage, boolean lastFromStaff, Instant createdAt,
                                        Instant updatedAt) {

        public static TicketSummaryResponse from(TicketView view) {
            Ticket ticket = view.ticket();
            List<TicketMessage> messages = ticket.getMessages();
            TicketMessage last = messages.isEmpty() ? null : messages.get(messages.size() - 1);
            return new TicketSummaryResponse(ticket.getId(), view.handle(), view.mine(),
                    ticket.getCategory().name(), Texts.of(ticket.getCategory().displayName()),
                    ticket.getSubject(), ticket.getStatus().name(), Texts.of(ticket.getStatus().displayName()),
                    messages.size(), last == null ? null : excerpt(last.getBody()),
                    last != null && last.isFromStaff(), ticket.getCreatedAt(), ticket.getUpdatedAt());
        }

        /** Extrait d'une ligne : la liste montre de quoi il s'agit, pas le message. */
        private static String excerpt(String body) {
            String oneLine = body.replaceAll("\\s+", " ").strip();
            return oneLine.length() <= 120 ? oneLine : oneLine.substring(0, 119) + "…";
        }
    }

    public record TicketResponse(Long id, String handle, boolean mine, String category, String categoryName,
                                 String subject, String status, String statusName, Instant createdAt,
                                 Instant updatedAt, List<MessageResponse> messages) {

        public static TicketResponse from(TicketView view) {
            Ticket ticket = view.ticket();
            return new TicketResponse(ticket.getId(), view.handle(), view.mine(), ticket.getCategory().name(),
                    Texts.of(ticket.getCategory().displayName()), ticket.getSubject(), ticket.getStatus().name(),
                    Texts.of(ticket.getStatus().displayName()), ticket.getCreatedAt(), ticket.getUpdatedAt(),
                    ticket.getMessages().stream().map(MessageResponse::from).toList());
        }
    }

    public record QueueResponse(List<TicketSummaryResponse> waiting, List<TicketSummaryResponse> answered,
                               List<TicketSummaryResponse> resolved, List<CategoryTallyResponse> byCategory,
                               Long medianMinutesToFirstReply) {

        public static QueueResponse from(SupportQueue queue) {
            return new QueueResponse(summaries(queue.waiting()), summaries(queue.answered()),
                    summaries(queue.resolved()),
                    queue.byCategory().stream().map(CategoryTallyResponse::from).toList(),
                    queue.medianMinutesToFirstReply());
        }

        private static List<TicketSummaryResponse> summaries(List<TicketView> views) {
            return views.stream().map(TicketSummaryResponse::from).toList();
        }
    }

    public record CategoryTallyResponse(String category, String categoryName, long count) {

        static CategoryTallyResponse from(SupportQueue.CategoryTally tally) {
            return new CategoryTallyResponse(tally.category(), Texts.of(tally.categoryName()), tally.count());
        }
    }
}
