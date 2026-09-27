package com.labplatform.domain.report;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportTotalsTest {

    @Test
    void unePeriodeSansAucunActeEstInactive() {
        assertTrue(ReportTotals.EMPTY.isIdle());
        assertFalse(new ReportTotals(0, 0, 0, 0, 0, 0, 0, 1, 1).isIdle());
    }

    /**
     * « Aucun quiz rendu » et « tous manqués » ne se lisent pas de la même
     * façon : le premier n'a rien à mesurer, le second est un zéro mérité.
     */
    @Test
    void sansQuizRenduLaReussiteNEstPasNulleMaisAbsente() {
        assertEquals(-1, ReportTotals.EMPTY.quizSuccessPercent());
        assertEquals(0, new ReportTotals(0, 0, 0, 0, 0, 3, 0, 1, 3).quizSuccessPercent());
    }

    @Test
    void laReussiteEstLaPartDesQuizReussis() {
        assertEquals(100, new ReportTotals(0, 0, 0, 0, 4, 0, 0, 1, 4).quizSuccessPercent());
        assertEquals(50, new ReportTotals(0, 0, 0, 0, 2, 2, 0, 1, 4).quizSuccessPercent());
        assertEquals(67, new ReportTotals(0, 0, 0, 0, 2, 1, 0, 1, 3).quizSuccessPercent());
    }
}
