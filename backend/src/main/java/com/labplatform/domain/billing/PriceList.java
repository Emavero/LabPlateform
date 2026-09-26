package com.labplatform.domain.billing;

import com.labplatform.domain.shared.InvalidInputException;

import java.util.Objects;

/** Tarif de la formule Pro, pour chacune des durées vendues. */
public record PriceList(Money monthly, Money yearly) {

    public PriceList {
        Objects.requireNonNull(monthly, "monthly");
        Objects.requireNonNull(yearly, "yearly");
        if (!monthly.currency().equals(yearly.currency())) {
            throw new InvalidInputException("Les deux durées doivent être facturées dans la même devise");
        }
    }

    public Money priceOf(BillingPeriod period) {
        return switch (Objects.requireNonNull(period, "period")) {
            case MONTHLY -> monthly;
            case YEARLY -> yearly;
        };
    }

    public String currencyCode() {
        return monthly.currencyCode();
    }
}
