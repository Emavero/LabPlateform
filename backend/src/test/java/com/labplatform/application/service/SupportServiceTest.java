package com.labplatform.application.service;

import com.labplatform.application.fakes.Fakes;
import com.labplatform.application.fakes.InMemoryJournal;
import com.labplatform.application.fakes.InMemoryTickets;
import com.labplatform.application.fakes.InMemoryUsers;
import com.labplatform.application.port.in.support.SupportQueue;
import com.labplatform.application.port.in.support.TicketView;
import com.labplatform.domain.journal.JournalKind;
import com.labplatform.domain.shared.ForbiddenException;
import com.labplatform.domain.shared.NotFoundException;
import com.labplatform.domain.support.TicketCategory;
import com.labplatform.domain.support.TicketStatus;
import com.labplatform.domain.user.Actor;
import com.labplatform.domain.user.Email;
import com.labplatform.domain.user.Role;
import com.labplatform.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-27T10:00:00Z");

    private final InMemoryJournal journal = new InMemoryJournal();
    private final InMemoryTickets tickets = new InMemoryTickets();
    private final InMemoryUsers users = new InMemoryUsers();

    private Actor alice;
    private Actor bob;
    private Actor admin;
    private MovingClock clock;
    private SupportService support;

    /** Horloge qu'un test avance : le délai de réponse n'a de sens qu'avec du temps. */
    private static final class MovingClock extends Clock {

        private Instant now;

        private MovingClock(Instant now) {
            this.now = now;
        }

        void advance(Duration by) {
            now = now.plus(by);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    @BeforeEach
    void setUp() {
        clock = new MovingClock(NOW);
        alice = actor("alice@lab.test", Role.USER);
        bob = actor("bob@lab.test", Role.USER);
        admin = actor("admin@lab.test", Role.ADMIN);
        support = new SupportService(tickets, users, journal, Fakes.NO_TRANSACTION, clock);
    }

    private Actor actor(String email, Role role) {
        User saved = users.save(User.register(Email.of(email), "empreinte", NOW));
        return new Actor(saved.getId(), role);
    }

    private TicketView openByAlice() {
        return support.open(alice, TicketCategory.BILLING, "Paiement Wave refusé", "Le paiement échoue à l'étape 2.");
    }

    @Test
    void ouvrirUneDemandeLInscritAuJournalAvecSaCategorie() {
        TicketView view = openByAlice();

        assertEquals(TicketStatus.OPEN, view.ticket().getStatus());
        assertTrue(view.mine());
        assertEquals("alice", view.handle());
        assertEquals(1L, journal.events.stream().filter(e -> e.getKind() == JournalKind.TICKET_OPENED).count());
    }

    @Test
    void leDemandeurNeVoitQueSesDemandes() {
        openByAlice();
        support.open(bob, TicketCategory.COURSES, "Vidéo muette", "La vidéo de la section 2 n'a pas de son.");

        assertEquals(1, support.listMine(alice).size());
        assertEquals(1, support.listMine(bob).size());
    }

    /** Lire la demande d'un autre répond « introuvable » : « interdit » révélerait son existence. */
    @Test
    void lireLaDemandeDUnAutreEstIntrouvable() {
        Long id = openByAlice().ticket().getId();

        assertThrows(NotFoundException.class, () -> support.get(bob, id));
    }

    @Test
    void lAdministrationLitToutesLesDemandes() {
        Long id = openByAlice().ticket().getId();

        TicketView seen = support.get(admin, id);

        assertFalse(seen.mine());
        assertEquals("alice", seen.handle());
    }

    @Test
    void laReponseDeLAdministrationEstMarqueeCommeVenantDeLEquipe() {
        Long id = openByAlice().ticket().getId();

        TicketView answered = support.reply(admin, id, "Quel opérateur mobile ?");

        assertEquals(TicketStatus.ANSWERED, answered.ticket().getStatus());
        assertTrue(answered.ticket().getMessages().get(1).isFromStaff());
        assertEquals(1L, journal.events.stream().filter(e -> e.getKind() == JournalKind.TICKET_REPLIED).count());
    }

    /**
     * Un administrateur est aussi un utilisateur : sa propre demande ne doit pas
     * s'afficher comme déjà traitée, sinon elle sortirait de la file sans avoir
     * été lue par personne.
     */
    @Test
    void laDemandeDUnAdministrateurPourLuiMemeResteOuverte() {
        Long id = support.open(admin, TicketCategory.LAB, "VPN coupé", "Plus de route vers le lab.").ticket().getId();

        TicketView after = support.reply(admin, id, "Toujours rien.");

        assertFalse(after.ticket().getMessages().get(1).isFromStaff());
        assertEquals(TicketStatus.OPEN, after.ticket().getStatus());
    }

    @Test
    void leDemandeurPeutCloreSaDemande() {
        Long id = openByAlice().ticket().getId();

        assertEquals(TicketStatus.RESOLVED, support.resolve(alice, id).ticket().getStatus());
    }

    @Test
    void laFileNEstLisibleQueParLAdministration() {
        assertThrows(ForbiddenException.class, () -> support.queue(alice));
    }

    @Test
    void laFileClasseLesDemandesParCeQuellesAttendent() {
        Long attente = openByAlice().ticket().getId();
        Long repondue = support.open(bob, TicketCategory.COURSES, "Vidéo muette", "Pas de son.").ticket().getId();
        support.reply(admin, repondue, "Quel navigateur ?");
        Long close = support.open(bob, TicketCategory.OTHER, "Merci", "Rien à signaler.").ticket().getId();
        support.resolve(bob, close);

        SupportQueue queue = support.queue(admin);

        assertEquals(List.of(attente), ids(queue.waiting()));
        assertEquals(List.of(repondue), ids(queue.answered()));
        assertEquals(List.of(close), ids(queue.resolved()));
    }

    /** La file se traite du plus ancien au plus récent : c'est le plus ancien qui attend le plus. */
    @Test
    void laFileDAttentePresenteLePlusAncienDAbord() {
        Long premier = openByAlice().ticket().getId();
        clock.advance(Duration.ofHours(3));
        Long second = support.open(bob, TicketCategory.MACHINES, "Cible injoignable", "Pas de ping.").ticket().getId();

        assertEquals(List.of(premier, second), ids(support.queue(admin).waiting()));
    }

    @Test
    void laFileCompteLesDemandesParCategorie() {
        openByAlice();
        support.open(bob, TicketCategory.BILLING, "Facture", "Où trouver ma facture ?");
        support.open(bob, TicketCategory.COURSES, "Vidéo muette", "Pas de son.");

        List<SupportQueue.CategoryTally> tallies = support.queue(admin).byCategory();

        assertEquals(2, tallies.size());
        assertEquals("BILLING", tallies.get(0).category());
        assertEquals(2L, tallies.get(0).count());
        assertEquals("COURSES", tallies.get(1).category());
    }

    @Test
    void leDelaiDeReponseEstLaMedianeDesPremieresReponses() {
        Long premier = openByAlice().ticket().getId();
        clock.advance(Duration.ofMinutes(10));
        support.reply(admin, premier, "Nous regardons.");

        Long second = support.open(bob, TicketCategory.MACHINES, "Cible injoignable", "Pas de ping.").ticket().getId();
        clock.advance(Duration.ofMinutes(30));
        support.reply(admin, second, "Le VPN est-il monté ?");

        assertEquals(20L, support.queue(admin).medianMinutesToFirstReply());
    }

    /** Sans réponse, le délai n'est pas zéro : il n'y a rien à mesurer. */
    @Test
    void sansAucuneReponseLeDelaiEstAbsent() {
        openByAlice();

        assertNull(support.queue(admin).medianMinutesToFirstReply());
    }

    private static List<Long> ids(List<TicketView> views) {
        return views.stream().map(view -> view.ticket().getId()).toList();
    }
}
