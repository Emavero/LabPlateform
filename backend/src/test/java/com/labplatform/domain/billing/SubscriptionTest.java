package com.labplatform.domain.billing;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubscriptionTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    @Test
    void anAccountWithoutAnyPaymentIsFree() {
        Subscription free = Subscription.free(42L);

        assertEquals(Plan.FREE, free.planAt(NOW));
        assertFalse(free.isActiveAt(NOW));
        assertEquals(SubscriptionStatus.EXPIRED, free.statusAt(NOW));
    }

    @Test
    void payingAMonthOpensProUntilTheSameDayOfTheNextMonth() {
        Subscription subscription = Subscription.free(42L);

        subscription.extend(BillingPeriod.MONTHLY, NOW);

        assertEquals(Plan.PRO, subscription.planAt(NOW));
        assertEquals(Instant.parse("2026-02-15T10:00:00Z"), subscription.getExpiresAt());
        assertEquals(SubscriptionStatus.ACTIVE, subscription.statusAt(NOW));
    }

    /** Renouveler d'avance ne doit pas faire perdre les jours restants. */
    @Test
    void renewingEarlyExtendsTheRemainingTermInsteadOfRestartingIt() {
        Subscription subscription = Subscription.free(42L);
        subscription.extend(BillingPeriod.MONTHLY, NOW);

        subscription.extend(BillingPeriod.MONTHLY, NOW.plusSeconds(10 * 86_400));

        assertEquals(Instant.parse("2026-03-15T10:00:00Z"), subscription.getExpiresAt());
    }

    @Test
    void renewingAfterALapseStartsFromTheDayOfPayment() {
        Subscription subscription = Subscription.free(42L);
        subscription.extend(BillingPeriod.MONTHLY, NOW);
        Instant muchLater = Instant.parse("2026-06-01T10:00:00Z");

        subscription.extend(BillingPeriod.MONTHLY, muchLater);

        assertEquals(Instant.parse("2026-07-01T10:00:00Z"), subscription.getExpiresAt());
        assertEquals(muchLater, subscription.getStartedAt());
    }

    /** Ce qui est payé reste dû : la résiliation coupe le renouvellement, pas l'accès. */
    @Test
    void cancellingKeepsAccessUntilTheTermAlreadyPaid() {
        Subscription subscription = Subscription.free(42L);
        subscription.extend(BillingPeriod.MONTHLY, NOW);

        subscription.cancel(NOW);

        assertTrue(subscription.isActiveAt(NOW.plusSeconds(86_400)));
        assertEquals(SubscriptionStatus.CANCELLED, subscription.statusAt(NOW));
        assertFalse(subscription.isActiveAt(Instant.parse("2026-03-01T10:00:00Z")));
    }

    /** L'état stocké n'est jamais cru sur parole face à la date d'échéance. */
    @Test
    void anActiveSubscriptionPastItsTermGrantsNothing() {
        Subscription stale = Subscription.restore(1L, 42L, Plan.PRO, SubscriptionStatus.ACTIVE,
                BillingPeriod.MONTHLY, NOW.minusSeconds(60 * 86_400), NOW.minusSeconds(86_400));

        assertFalse(stale.isActiveAt(NOW));
        assertEquals(Plan.FREE, stale.planAt(NOW));
        assertEquals(SubscriptionStatus.EXPIRED, stale.statusAt(NOW));
    }

    @Test
    void ayearIsBilledAsACalendarYear() {
        Subscription subscription = Subscription.free(42L);

        subscription.extend(BillingPeriod.YEARLY, NOW);

        assertEquals(Instant.parse("2027-01-15T10:00:00Z"), subscription.getExpiresAt());
    }
}
