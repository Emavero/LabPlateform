package com.labplatform.domain.report;

import java.time.Instant;
import java.util.List;

/**
 * Rapport d'activité d'un compte sur une période.
 * <p>
 * Tout y est calculé, rien n'y est stocké : un rapport enregistré vieillirait
 * mal — le catalogue change, les cours sont réécrits — et il faudrait le
 * régénérer pour s'y fier. Le recalculer à la demande coûte une requête et dit
 * toujours la vérité du moment.
 *
 * @param totals   ce qui a été fait sur la période
 * @param previous la même mesure sur la période précédente, pour la tendance
 * @param families activité par famille d'usage : où le compte passe son temps
 * @param kinds    natures d'actes les plus fréquentes
 * @param machines machines possédées sur la période
 * @param courses  cours travaillés sur la période
 */
public record ActivityReport(Instant from, Instant to, int days, ReportTotals totals, ReportTotals previous,
                             List<ReportTally> families, List<ReportTally> kinds, List<MachineLine> machines,
                             List<CourseLine> courses) {

    /**
     * Machine du rapport.
     *
     * @param pwned les deux flags ont été validés sur la période
     */
    public record MachineLine(String slug, String name, String difficulty, long flags, long points, boolean pwned,
                              Instant lastAt) {
    }

    /**
     * Cours du rapport.
     *
     * @param sections sections terminées sur la période
     * @param minutes  durée annoncée de ces sections
     */
    public record CourseLine(String slug, String title, String track, long sections, long minutes, Instant lastAt) {
    }
}
