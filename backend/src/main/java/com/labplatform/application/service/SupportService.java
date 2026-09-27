package com.labplatform.application.service;

import com.labplatform.application.port.in.support.GetSupportQueueUseCase;
import com.labplatform.application.port.in.support.ListTicketsUseCase;
import com.labplatform.application.port.in.support.OpenTicketUseCase;
import com.labplatform.application.port.in.support.ReplyToTicketUseCase;
import com.labplatform.application.port.in.support.SupportQueue;
import com.labplatform.application.port.in.support.TicketView;
import com.labplatform.application.port.out.JournalPort;
import com.labplatform.application.port.out.TicketRepositoryPort;
import com.labplatform.application.port.out.TransactionPort;
import com.labplatform.application.port.out.UserRepositoryPort;
import com.labplatform.domain.journal.JournalEvent;
import com.labplatform.domain.journal.JournalKind;
import com.labplatform.domain.scoring.Handle;
import com.labplatform.domain.shared.ForbiddenException;
import com.labplatform.domain.shared.NotFoundException;
import com.labplatform.domain.support.Ticket;
import com.labplatform.domain.support.TicketCategory;
import com.labplatform.domain.support.TicketMessage;
import com.labplatform.domain.support.TicketStatus;
import com.labplatform.domain.user.Actor;
import com.labplatform.domain.user.User;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Assistance : des demandes, et le fil de leur traitement.
 * <p>
 * Deux règles portent tout le module. La première : l'équipe lit tout, un
 * demandeur ne lit que ses demandes — une demande contient des copies d'écran
 * et des messages d'erreur, ce n'est pas une conversation publique. La
 * seconde : un message est marqué « de l'équipe » quand un administrateur
 * écrit dans la demande d'un <em>autre</em>, jamais quand il écrit dans la
 * sienne, sans quoi sa propre demande s'afficherait comme déjà traitée.
 */
public class SupportService implements OpenTicketUseCase, ListTicketsUseCase, ReplyToTicketUseCase,
        GetSupportQueueUseCase {

    /** File de l'équipe : le plus ancien d'abord, c'est celui qui attend le plus. */
    private static final Comparator<TicketView> OLDEST_FIRST =
            Comparator.comparing(view -> view.ticket().getUpdatedAt());
    private static final Comparator<TicketView> NEWEST_FIRST = OLDEST_FIRST.reversed();

    private final TicketRepositoryPort tickets;
    private final UserRepositoryPort users;
    private final JournalPort journal;
    private final TransactionPort transactions;
    private final Clock clock;

    public SupportService(TicketRepositoryPort tickets, UserRepositoryPort users, JournalPort journal,
                          TransactionPort transactions, Clock clock) {
        this.tickets = tickets;
        this.users = users;
        this.journal = journal;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public TicketView open(Actor actor, TicketCategory category, String subject, String body) {
        Ticket saved = transactions.inTransaction(
                () -> tickets.save(Ticket.open(actor.userId(), category, subject, body, clock.instant())));
        journal.record(JournalEvent.of(actor.userId(), JournalKind.TICKET_OPENED, String.valueOf(saved.getId()),
                saved.getCategory().displayName(), clock.instant()));
        return view(saved, actor);
    }

    @Override
    public List<TicketView> listMine(Actor actor) {
        return tickets.findByAuthor(actor.userId()).stream()
                .map(ticket -> view(ticket, actor))
                .sorted(NEWEST_FIRST)
                .toList();
    }

    @Override
    public TicketView get(Actor actor, Long ticketId) {
        return view(readable(actor, ticketId), actor);
    }

    @Override
    public TicketView reply(Actor actor, Long ticketId, String body) {
        Ticket ticket = readable(actor, ticketId);
        boolean fromStaff = actor.isAdmin() && !ticket.isOpenedBy(actor.userId());
        Ticket saved = transactions.inTransaction(() -> {
            ticket.reply(actor.userId(), fromStaff, body, clock.instant());
            return tickets.save(ticket);
        });
        journal.record(JournalEvent.of(actor.userId(), JournalKind.TICKET_REPLIED, String.valueOf(ticketId),
                clock.instant()));
        return view(saved, actor);
    }

    @Override
    public TicketView resolve(Actor actor, Long ticketId) {
        Ticket ticket = readable(actor, ticketId);
        return view(transactions.inTransaction(() -> {
            // Sans changement d'état, rien à écrire : une demande déjà close
            // ne doit pas remonter en tête de liste pour un clic de trop.
            return ticket.resolve(clock.instant()) ? tickets.save(ticket) : ticket;
        }), actor);
    }

    @Override
    public SupportQueue queue(Actor actor) {
        requireStaff(actor);
        List<TicketView> all = tickets.findAll().stream().map(ticket -> view(ticket, actor)).toList();
        return new SupportQueue(
                byStatus(all, TicketStatus.OPEN, OLDEST_FIRST),
                byStatus(all, TicketStatus.ANSWERED, NEWEST_FIRST),
                byStatus(all, TicketStatus.RESOLVED, NEWEST_FIRST),
                tally(all),
                medianMinutesToFirstReply(all));
    }

    private static List<TicketView> byStatus(List<TicketView> all, TicketStatus status, Comparator<TicketView> order) {
        return all.stream().filter(view -> view.ticket().getStatus() == status).sorted(order).toList();
    }

    /** Catégories dans l'ordre de l'énumération : une file se compare d'une semaine à l'autre. */
    private static List<SupportQueue.CategoryTally> tally(List<TicketView> all) {
        Map<TicketCategory, Long> counts = new java.util.EnumMap<>(TicketCategory.class);
        all.forEach(view -> counts.merge(view.ticket().getCategory(), 1L, Long::sum));
        List<SupportQueue.CategoryTally> tallies = new ArrayList<>();
        for (TicketCategory category : TicketCategory.values()) {
            long count = counts.getOrDefault(category, 0L);
            if (count > 0) {
                tallies.add(new SupportQueue.CategoryTally(category.name(), category.displayName(), count));
            }
        }
        return tallies;
    }

    /**
     * Délai médian de première réponse, et non moyen : une demande oubliée une
     * semaine tirerait la moyenne au point de la rendre illisible, alors que la
     * médiane continue de décrire le traitement habituel.
     */
    private static Long medianMinutesToFirstReply(List<TicketView> all) {
        List<Long> delays = all.stream()
                .map(view -> firstReplyDelay(view.ticket()))
                .flatMap(Optional::stream)
                .sorted()
                .toList();
        if (delays.isEmpty()) {
            return null;
        }
        int middle = delays.size() / 2;
        return delays.size() % 2 == 1
                ? delays.get(middle)
                : (delays.get(middle - 1) + delays.get(middle)) / 2;
    }

    private static Optional<Long> firstReplyDelay(Ticket ticket) {
        List<TicketMessage> messages = ticket.getMessages();
        if (messages.isEmpty()) {
            return Optional.empty();
        }
        Instant asked = messages.get(0).getSentAt();
        return messages.stream()
                .filter(TicketMessage::isFromStaff)
                .map(TicketMessage::getSentAt)
                .findFirst()
                .map(answered -> Duration.between(asked, answered).toMinutes());
    }

    private Ticket readable(Actor actor, Long ticketId) {
        Ticket ticket = tickets.find(ticketId).orElseThrow(() -> new NotFoundException("Demande introuvable"));
        if (!ticket.isReadableBy(actor.userId(), actor.isAdmin())) {
            // Introuvable, et non « interdit » : répondre « interdit » révélerait
            // qu'une demande existe sous cet identifiant.
            throw new NotFoundException("Demande introuvable");
        }
        return ticket;
    }

    private static void requireStaff(Actor actor) {
        if (!actor.isAdmin()) {
            throw new ForbiddenException("Réservé à l'administration");
        }
    }

    private TicketView view(Ticket ticket, Actor actor) {
        String handle = users.findById(ticket.getAuthorId())
                .map(User::getEmail)
                .map(email -> Handle.fromEmail(email.value()))
                .orElse("anonyme");
        return new TicketView(ticket, handle, ticket.isOpenedBy(actor.userId()));
    }
}
