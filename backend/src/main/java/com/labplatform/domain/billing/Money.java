package com.labplatform.domain.billing;

import com.labplatform.domain.shared.InvalidInputException;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * Montant, tenu en unités indivisibles de sa devise (centimes pour l'euro,
 * francs entiers pour le XOF qui n'a pas de subdivision).
 * <p>
 * Jamais un {@code double} : additionner des flottants fait perdre des
 * centimes, et un prix affiché qui ne correspond pas au prélèvement est un
 * litige. Le nombre de décimales vient de la devise elle-même, ce qui évite
 * de coder en dur qu'un montant a « deux chiffres après la virgule ».
 */
public record Money(long minorUnits, Currency currency) {

    public Money {
        Objects.requireNonNull(currency, "currency");
        if (minorUnits < 0) {
            throw new InvalidInputException("Un montant ne peut pas être négatif");
        }
    }

    /** Depuis un montant écrit comme on l'affiche : {@code of("EUR", "8.00")}. */
    public static Money of(String currencyCode, BigDecimal major) {
        Currency currency = currencyOf(currencyCode);
        Objects.requireNonNull(major, "major");
        int digits = Math.max(currency.getDefaultFractionDigits(), 0);
        BigDecimal scaled = major.movePointRight(digits);
        if (scaled.stripTrailingZeros().scale() > 0) {
            throw new InvalidInputException("Le montant " + major + " est plus précis que la devise " + currencyCode);
        }
        return new Money(scaled.longValueExact(), currency);
    }

    public static Money zero(String currencyCode) {
        return new Money(0L, currencyOf(currencyCode));
    }

    public static Currency currencyOf(String code) {
        try {
            return Currency.getInstance(Objects.requireNonNull(code, "code").trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException unknown) {
            throw new InvalidInputException("Devise inconnue : " + code);
        }
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(minorUnits + other.minorUnits, currency);
    }

    /** Montant tel qu'on l'écrit et tel que l'attendent les API de paiement. */
    public BigDecimal toDecimal() {
        return BigDecimal.valueOf(minorUnits, Math.max(currency.getDefaultFractionDigits(), 0));
    }

    public String currencyCode() {
        return currency.getCurrencyCode();
    }

    public boolean isZero() {
        return minorUnits == 0;
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new InvalidInputException("Impossible d'additionner des " + currencyCode() + " et des "
                    + other.currencyCode());
        }
    }
}
