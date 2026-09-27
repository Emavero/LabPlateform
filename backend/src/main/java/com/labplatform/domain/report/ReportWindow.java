package com.labplatform.domain.report;

import com.labplatform.domain.shared.InvalidInputException;

import java.time.Duration;
import java.time.Instant;

/**
 * Période d'un rapport, et la période équivalente qui la précède.
 * <p>
 * Les deux vont ensemble : un chiffre seul ne dit pas s'il monte ou s'il
 * descend, et comparer à « depuis toujours » comparerait une semaine à des
 * mois. La période précédente a donc exactement la même durée, collée à la
 * borne inférieure.
 * <p>
 * Bornes : la borne inférieure est exclue, la supérieure incluse. Deux périodes
 * qui se suivent ne comptent ainsi jamais deux fois le même événement.
 */
public record ReportWindow(Instant from, Instant to, int days) {

    /** Bornes acceptées : une semaine au moins, une année au plus. */
    public static final int MIN_DAYS = 7;
    public static final int MAX_DAYS = 365;

    public ReportWindow {
        if (from == null || to == null) {
            throw new InvalidInputException("La période du rapport est incomplète");
        }
        if (!from.isBefore(to)) {
            throw new InvalidInputException("La période du rapport est vide");
        }
    }

    /**
     * Période des {@code days} derniers jours. Une demande hors bornes est
     * ramenée dans les bornes plutôt que refusée : un rapport est une lecture,
     * et un paramètre d'URL tordu ne doit pas faire échouer une page.
     */
    public static ReportWindow lastDays(Instant now, int days) {
        int bounded = Math.max(MIN_DAYS, Math.min(MAX_DAYS, days));
        return new ReportWindow(now.minus(Duration.ofDays(bounded)), now, bounded);
    }

    /** La période de même durée qui précède celle-ci. */
    public ReportWindow previous() {
        return new ReportWindow(from.minus(Duration.ofDays(days)), from, days);
    }

    public boolean contains(Instant moment) {
        return moment != null && moment.isAfter(from) && !moment.isAfter(to);
    }
}
