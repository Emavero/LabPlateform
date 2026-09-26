package com.labplatform.domain.billing;

import com.labplatform.domain.shared.ConflictException;
import com.labplatform.domain.shared.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentTest {

    private static final String REFERENCE = "0123456789abcdef0123456789abcdef";
    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    private static Payment pending() {
        return Payment.initiate(REFERENCE, 42L, BillingPeriod.MONTHLY, Money.of("XOF", new BigDecimal("5000")),
                PaymentMethod.WAVE, NOW);
    }

    /**
     * Le cœur de la facturation : les prestataires répètent leurs
     * notifications, et un mois crédité deux fois est un mois offert.
     */
    @Test
    void theSameConfirmationTwiceOnlyCreditsOnce() {
        Payment payment = pending();

        assertTrue(payment.succeed(NOW));
        assertFalse(payment.succeed(NOW.plusSeconds(60)));
        assertTrue(payment.hasSucceeded());
    }

    @Test
    void aSettledPaymentDoesNotGoBackwards() {
        Payment paid = pending();
        paid.succeed(NOW);
        assertThrows(ConflictException.class, () -> paid.fail("trop tard", NOW));

        Payment failed = pending();
        failed.fail("refusé", NOW);
        assertThrows(ConflictException.class, () -> failed.succeed(NOW));
    }

    @Test
    void repeatingTheSameFailureOrCancellationIsHarmless() {
        Payment failed = pending();
        failed.fail("refusé", NOW);
        failed.fail("refusé", NOW);
        assertEquals(PaymentStatus.FAILED, failed.getStatus());

        Payment cancelled = pending();
        cancelled.cancel(NOW);
        cancelled.cancel(NOW);
        assertEquals(PaymentStatus.CANCELLED, cancelled.getStatus());
    }

    /** La référence circule dans les URL : devinable, elle laisserait confirmer le paiement d'autrui. */
    @Test
    void refusesAReferenceThatIsNotThirtyTwoHexCharacters() {
        assertThrows(InvalidInputException.class, () -> Payment.initiate("court", 42L, BillingPeriod.MONTHLY,
                Money.of("XOF", new BigDecimal("5000")), PaymentMethod.WAVE, NOW));
    }

    @Test
    void refusesToChargeNothing() {
        assertThrows(InvalidInputException.class, () -> Payment.initiate(REFERENCE, 42L, BillingPeriod.MONTHLY,
                Money.zero("XOF"), PaymentMethod.WAVE, NOW));
    }

    @Test
    void knowsWhoseItIs() {
        Payment payment = pending();

        assertTrue(payment.isOwnedBy(42L));
        assertFalse(payment.isOwnedBy(43L));
    }
}
