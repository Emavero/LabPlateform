package com.labplatform.domain.billing;

import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;

/**
 * Durée achetée en une fois. Les mois ne font pas tous le même nombre de
 * jours : l'échéance est donc calculée en mois calendaires, pas en multiples
 * de 30 jours, pour qu'un abonnement souscrit le 31 janvier tombe bien au
 * 28 février et non au 2 mars.
 */
public enum BillingPeriod {

    MONTHLY("Mensuel", Period.ofMonths(1)),
    YEARLY("Annuel", Period.ofYears(1));

    private final String displayName;
    private final Period length;

    BillingPeriod(String displayName, Period length) {
        this.displayName = displayName;
        this.length = length;
    }

    public String displayName() {
        return displayName;
    }

    public Instant addTo(Instant start) {
        return start.atZone(ZoneOffset.UTC).plus(length).toInstant();
    }
}
