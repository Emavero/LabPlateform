package com.labplatform.domain.support;

import com.labplatform.domain.shared.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketTest {

    private static final Instant NOW = Instant.parse("2026-09-27T10:00:00Z");
    private static final Long ASKER = 7L;
    private static final Long STAFF = 1L;

    private static Ticket open() {
        return Ticket.open(ASKER, TicketCategory.BILLING, "Paiement Wave refusé", "Le paiement échoue à l'étape 2.",
                NOW);
    }

    @Test
    void uneDemandeNaitOuverteAvecSonPremierMessage() {
        Ticket ticket = open();

        assertEquals(TicketStatus.OPEN, ticket.getStatus());
        assertEquals(1, ticket.getMessageCount());
        assertFalse(ticket.getMessages().get(0).isFromStaff());
        assertFalse(ticket.hasStaffReply());
    }

    @Test
    void laReponseDeLEquipeRendLaDemandeAuDemandeur() {
        Ticket ticket = open();

        ticket.reply(STAFF, true, "Quel opérateur ?", NOW.plus(Duration.ofMinutes(20)));

        assertEquals(TicketStatus.ANSWERED, ticket.getStatus());
        assertTrue(ticket.hasStaffReply());
        assertEquals(NOW.plus(Duration.ofMinutes(20)), ticket.getUpdatedAt());
    }

    @Test
    void laReponseDuDemandeurLaRendALEquipe() {
        Ticket ticket = open();
        ticket.reply(STAFF, true, "Quel opérateur ?", NOW.plus(Duration.ofMinutes(20)));

        ticket.reply(ASKER, false, "Orange.", NOW.plus(Duration.ofMinutes(30)));

        assertEquals(TicketStatus.OPEN, ticket.getStatus());
    }

    /** Une demande close n'est pas une impasse : « c'est toujours cassé » la rouvre. */
    @Test
    void unMessageApresResolutionRouvreLaDemande() {
        Ticket ticket = open();
        assertTrue(ticket.resolve(NOW.plus(Duration.ofHours(1))));
        assertEquals(TicketStatus.RESOLVED, ticket.getStatus());

        ticket.reply(ASKER, false, "Cela recommence.", NOW.plus(Duration.ofHours(2)));

        assertEquals(TicketStatus.OPEN, ticket.getStatus());
        assertEquals(2, ticket.getMessageCount());
    }

    @Test
    void resoudreDeuxFoisNeChangeRien() {
        Ticket ticket = open();
        ticket.resolve(NOW.plus(Duration.ofHours(1)));

        assertFalse(ticket.resolve(NOW.plus(Duration.ofHours(2))));
        assertEquals(NOW.plus(Duration.ofHours(1)), ticket.getUpdatedAt());
    }

    @Test
    void leDemandeurLitLaSienneLEquipeLitTout() {
        Ticket ticket = open();

        assertTrue(ticket.isReadableBy(ASKER, false));
        assertFalse(ticket.isReadableBy(99L, false));
        assertTrue(ticket.isReadableBy(99L, true));
    }

    @Test
    void unSujetVideEstRefuse() {
        assertThrows(InvalidInputException.class,
                () -> Ticket.open(ASKER, TicketCategory.OTHER, "  ", "Un message.", NOW));
    }

    @Test
    void unMessageVideEstRefuse() {
        assertThrows(InvalidInputException.class,
                () -> Ticket.open(ASKER, TicketCategory.OTHER, "Sujet", "   ", NOW));
    }

    @Test
    void unSujetTropLongEstRefuse() {
        assertThrows(InvalidInputException.class,
                () -> Ticket.open(ASKER, TicketCategory.OTHER, "s".repeat(141), "Un message.", NOW));
    }

    /** Le fil ne se réécrit pas depuis l'extérieur de l'agrégat. */
    @Test
    void leFilNEstPasModifiable() {
        Ticket ticket = open();

        assertThrows(UnsupportedOperationException.class,
                () -> ticket.getMessages().add(ticket.getMessages().get(0)));
    }
}
