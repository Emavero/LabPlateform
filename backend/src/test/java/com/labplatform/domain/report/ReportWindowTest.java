package com.labplatform.domain.report;

import com.labplatform.domain.shared.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportWindowTest {

    private static final Instant NOW = Instant.parse("2026-09-27T12:00:00Z");

    @Test
    void laPeriodeCouvreLesDerniersJoursDemandes() {
        ReportWindow window = ReportWindow.lastDays(NOW, 30);

        assertEquals(30, window.days());
        assertEquals(NOW, window.to());
        assertEquals(NOW.minus(Duration.ofDays(30)), window.from());
    }

    /** Un paramètre d'URL tordu ne doit pas faire échouer une page de lecture. */
    @Test
    void uneDureeHorsBornesEstRameneeDansLesBornes() {
        assertEquals(ReportWindow.MIN_DAYS, ReportWindow.lastDays(NOW, 0).days());
        assertEquals(ReportWindow.MIN_DAYS, ReportWindow.lastDays(NOW, -40).days());
        assertEquals(ReportWindow.MAX_DAYS, ReportWindow.lastDays(NOW, 10_000).days());
    }

    @Test
    void laPeriodePrecedenteALaMemeDureeEtSArreteAuDebut() {
        ReportWindow window = ReportWindow.lastDays(NOW, 14);

        ReportWindow before = window.previous();

        assertEquals(14, before.days());
        assertEquals(window.from(), before.to());
        assertEquals(window.from().minus(Duration.ofDays(14)), before.from());
    }

    /**
     * Borne inférieure exclue, supérieure incluse : deux périodes qui se suivent
     * ne comptent jamais deux fois le même événement.
     */
    @Test
    void lesBornesNeSeChevauchentPas() {
        ReportWindow window = ReportWindow.lastDays(NOW, 7);
        ReportWindow before = window.previous();

        assertFalse(window.contains(window.from()));
        assertTrue(before.contains(window.from()));
        assertTrue(window.contains(window.to()));
    }

    @Test
    void unMomentHorsPeriodeNEnFaitPasPartie() {
        ReportWindow window = ReportWindow.lastDays(NOW, 7);

        assertFalse(window.contains(NOW.plus(Duration.ofSeconds(1))));
        assertFalse(window.contains(NOW.minus(Duration.ofDays(8))));
        assertFalse(window.contains(null));
    }

    @Test
    void unePeriodeVideEstRefusee() {
        assertThrows(InvalidInputException.class, () -> new ReportWindow(NOW, NOW, 0));
        assertThrows(InvalidInputException.class, () -> new ReportWindow(NOW, NOW.minusSeconds(1), 1));
        assertThrows(InvalidInputException.class, () -> new ReportWindow(null, NOW, 7));
    }
}
