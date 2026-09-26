package com.labplatform.domain.billing;

import com.labplatform.domain.shared.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {

    @Test
    void readsTheNumberOfDecimalsFromTheCurrencyItself() {
        assertEquals(800L, Money.of("EUR", new BigDecimal("8.00")).minorUnits());
        // Le franc CFA n'a pas de subdivision : 5 000 francs valent 5 000 unités.
        assertEquals(5_000L, Money.of("XOF", new BigDecimal("5000")).minorUnits());
    }

    @Test
    void writesBackTheAmountAsItIsDisplayed() {
        assertEquals("8.00", Money.of("EUR", new BigDecimal("8")).toDecimal().toPlainString());
        assertEquals("5000", Money.of("XOF", new BigDecimal("5000")).toDecimal().toPlainString());
    }

    /** Sans quoi on facturerait 8 € pour 8,49 € demandés. */
    @Test
    void refusesAnAmountMorePreciseThanItsCurrency() {
        assertThrows(InvalidInputException.class, () -> Money.of("XOF", new BigDecimal("5000.50")));
        assertThrows(InvalidInputException.class, () -> Money.of("EUR", new BigDecimal("8.005")));
    }

    @Test
    void refusesAnUnknownCurrencyAndANegativeAmount() {
        assertThrows(InvalidInputException.class, () -> Money.of("XYZ", BigDecimal.ONE));
        assertThrows(InvalidInputException.class, () -> new Money(-1L, Money.currencyOf("EUR")));
    }

    @Test
    void refusesToAddTwoDifferentCurrencies() {
        Money euros = Money.of("EUR", new BigDecimal("8.00"));
        Money francs = Money.of("XOF", new BigDecimal("5000"));
        assertThrows(InvalidInputException.class, () -> euros.plus(francs));
        assertEquals(1_600L, euros.plus(euros).minorUnits());
    }
}
