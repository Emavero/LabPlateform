package com.labplatform.application.service;

import com.labplatform.application.fakes.Fakes;
import com.labplatform.application.fakes.InMemoryJournal;
import com.labplatform.application.fakes.InMemoryPayments;
import com.labplatform.application.fakes.InMemorySubscriptions;
import com.labplatform.application.fakes.ScriptedPaymentGateway;
import com.labplatform.application.port.in.billing.BillingView;
import com.labplatform.application.port.in.billing.CheckoutTicket;
import com.labplatform.domain.billing.BillingPeriod;
import com.labplatform.domain.billing.Money;
import com.labplatform.domain.billing.PaymentMethod;
import com.labplatform.domain.billing.PaymentStatus;
import com.labplatform.domain.billing.Plan;
import com.labplatform.domain.billing.PriceList;
import com.labplatform.domain.billing.SubscriptionStatus;
import com.labplatform.domain.shared.ConflictException;
import com.labplatform.domain.shared.InvalidInputException;
import com.labplatform.domain.shared.NotFoundException;
import com.labplatform.domain.shared.ServiceUnavailableException;
import com.labplatform.domain.user.Actor;
import com.labplatform.domain.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BillingServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final Actor ALICE = new Actor(1L, Role.USER);
    private static final Actor BOB = new Actor(2L, Role.USER);
    private static final Actor ADMIN = new Actor(9L, Role.ADMIN);
    private static final String REFERENCE = "aaaabbbbccccddddeeeeffff00001111";

    private final InMemoryJournal journal = new InMemoryJournal();
    private InMemorySubscriptions subscriptions;
    private InMemoryPayments payments;
    private ScriptedPaymentGateway gateway;
    private BillingService billing;

    @BeforeEach
    void setUp() {
        subscriptions = new InMemorySubscriptions();
        payments = new InMemoryPayments();
        gateway = new ScriptedPaymentGateway();
        billing = service(settings(true));
    }

    private BillingService service(BillingSettings settings) {
        return new BillingService(subscriptions, payments, gateway, Fakes.secretGenerator(() -> "inutilisé",
                () -> REFERENCE), journal, Fakes.NO_TRANSACTION, Clock.fixed(NOW, ZoneOffset.UTC), settings);
    }

    private static BillingSettings settings(boolean enabled) {
        Map<PaymentMethod, PriceList> overrides = new EnumMap<>(PaymentMethod.class);
        overrides.put(PaymentMethod.CARD, new PriceList(Money.of("EUR", new BigDecimal("8.00")),
                Money.of("EUR", new BigDecimal("80.00"))));
        return new BillingSettings(enabled,
                new PriceList(Money.of("XOF", new BigDecimal("5000")), Money.of("XOF", new BigDecimal("50000"))),
                overrides, Set.of(PaymentMethod.CARD, PaymentMethod.WAVE),
                "https://cybermans.test/retour", "https://cybermans.test/abonnement");
    }

    @Test
    void anAccountWithoutPaymentIsFreeAndSeesTheOffer() {
        BillingView view = billing.billingOf(ALICE);

        assertEquals(Plan.FREE, view.plan());
        // Deux moyens de paiement, deux durées.
        assertEquals(4, view.offers().size());
        assertTrue(view.payments().isEmpty());
    }

    /** Le franc CFA pour Wave, l'euro pour la carte : le tarif suit le moyen. */
    @Test
    void thePriceFollowsThePaymentMethod() {
        BillingView view = billing.billingOf(ALICE);

        assertEquals(5_000L, price(view, PaymentMethod.WAVE, BillingPeriod.MONTHLY).minorUnits());
        assertEquals("XOF", price(view, PaymentMethod.WAVE, BillingPeriod.MONTHLY).currencyCode());
        assertEquals(800L, price(view, PaymentMethod.CARD, BillingPeriod.MONTHLY).minorUnits());
        assertEquals("EUR", price(view, PaymentMethod.CARD, BillingPeriod.MONTHLY).currencyCode());
    }

    @Test
    void payingOpensProUntilTheTerm() {
        CheckoutTicket ticket = billing.startCheckout(ALICE, PaymentMethod.WAVE, BillingPeriod.MONTHLY);
        assertEquals(REFERENCE, ticket.reference());
        assertEquals("https://paiement.test/" + REFERENCE, ticket.redirectUrl());

        BillingView view = billing.confirm(ALICE, REFERENCE);

        assertEquals(Plan.PRO, view.plan());
        assertEquals(SubscriptionStatus.ACTIVE, view.status());
        assertEquals(Instant.parse("2026-02-15T10:00:00Z"), view.expiresAt());
        assertEquals(Plan.PRO, billing.planOf(ALICE));
    }

    /** Ce que l'idempotence protège : une deuxième confirmation n'offre pas un mois. */
    @Test
    void confirmingTwiceDoesNotCreditTwice() {
        billing.startCheckout(ALICE, PaymentMethod.WAVE, BillingPeriod.MONTHLY);
        Instant firstTerm = billing.confirm(ALICE, REFERENCE).expiresAt();

        Instant secondTerm = billing.confirm(ALICE, REFERENCE).expiresAt();

        assertEquals(firstTerm, secondTerm);
    }

    @Test
    void theSameNotificationTwiceDoesNotCreditTwice() {
        billing.startCheckout(ALICE, PaymentMethod.WAVE, BillingPeriod.MONTHLY);
        gateway.notifiedReference = REFERENCE;

        billing.applyNotification(PaymentMethod.WAVE, "signature", "corps");
        Instant firstTerm = billing.billingOf(ALICE).expiresAt();
        billing.applyNotification(PaymentMethod.WAVE, "signature", "corps");

        assertEquals(firstTerm, billing.billingOf(ALICE).expiresAt());
    }

    /** Une signature valide prouve que le prestataire parle, pas de quel paiement. */
    @Test
    void aNotificationPointingAtAnotherSessionIsIgnored() {
        billing.startCheckout(ALICE, PaymentMethod.WAVE, BillingPeriod.MONTHLY);
        gateway.notifiedReference = REFERENCE;
        gateway.notifiedProviderReference = "prov_autre_chose";

        billing.applyNotification(PaymentMethod.WAVE, "signature", "corps");

        assertEquals(Plan.FREE, billing.planOf(ALICE));
    }

    @Test
    void aNotificationForAnUnknownReferenceIsIgnoredWithoutFailing() {
        gateway.notifiedReference = "ffffffffffffffffffffffffffffffff";

        billing.applyNotification(PaymentMethod.WAVE, "signature", "corps");

        assertEquals(Plan.FREE, billing.planOf(ALICE));
    }

    /** L'application ne décide pas : si le prestataire dit « refusé », c'est refusé. */
    @Test
    void aRefusedPaymentGrantsNothingAndIsTraced() {
        billing.startCheckout(ALICE, PaymentMethod.WAVE, BillingPeriod.MONTHLY);
        gateway.nextStatus = PaymentStatus.FAILED;

        BillingView view = billing.confirm(ALICE, REFERENCE);

        assertEquals(Plan.FREE, view.plan());
        assertEquals(PaymentStatus.FAILED, view.payments().get(0).status());
    }

    @Test
    void aPaymentStillPendingAtTheProviderLeavesEverythingAsItIs() {
        billing.startCheckout(ALICE, PaymentMethod.WAVE, BillingPeriod.MONTHLY);
        gateway.nextStatus = PaymentStatus.PENDING;

        BillingView view = billing.confirm(ALICE, REFERENCE);

        assertEquals(Plan.FREE, view.plan());
        assertEquals(PaymentStatus.PENDING, view.payments().get(0).status());
    }

    /** Sinon une référence devinée donnerait l'abonnement de son voisin. */
    @Test
    void oneAccountCannotConfirmAnotherAccountsPayment() {
        billing.startCheckout(ALICE, PaymentMethod.WAVE, BillingPeriod.MONTHLY);

        assertThrows(NotFoundException.class, () -> billing.confirm(BOB, REFERENCE));
        assertEquals(Plan.FREE, billing.planOf(ALICE));
    }

    @Test
    void anUnreachableProviderLeavesAFailedPaymentAndNotAPendingOne() {
        gateway.unreachable = true;

        assertThrows(ServiceUnavailableException.class,
                () -> billing.startCheckout(ALICE, PaymentMethod.WAVE, BillingPeriod.MONTHLY));

        assertEquals(PaymentStatus.FAILED, billing.billingOf(ALICE).payments().get(0).status());
    }

    @Test
    void anUnservedMethodIsRefusedBeforeAnythingIsWritten() {
        assertThrows(InvalidInputException.class,
                () -> billing.startCheckout(ALICE, PaymentMethod.ORANGE_MONEY, BillingPeriod.MONTHLY));
        assertTrue(billing.billingOf(ALICE).payments().isEmpty());
    }

    @Test
    void cancellingStopsTheRenewalAndKeepsTheTerm() {
        billing.startCheckout(ALICE, PaymentMethod.WAVE, BillingPeriod.MONTHLY);
        billing.confirm(ALICE, REFERENCE);

        BillingView view = billing.cancel(ALICE);

        assertEquals(SubscriptionStatus.CANCELLED, view.status());
        assertFalse(view.renewing());
        assertNotNull(view.expiresAt());
        // L'accès court jusqu'à l'échéance déjà payée.
        assertEquals(Plan.PRO, billing.planOf(ALICE));
    }

    @Test
    void cancellingWithoutSubscriptionIsAConflict() {
        assertThrows(ConflictException.class, () -> billing.cancel(ALICE));
    }

    /** Un lab interne n'a pas de caisse : tout le monde y a accès à tout. */
    @Test
    void whenBillingIsOffEveryAccountIsTreatedAsPro() {
        BillingService free = service(settings(false));

        assertEquals(Plan.PRO, free.planOf(ALICE));
        assertEquals(Plan.PRO, free.billingOf(ALICE).plan());
        assertThrows(ConflictException.class,
                () -> free.startCheckout(ALICE, PaymentMethod.WAVE, BillingPeriod.MONTHLY));
    }

    @Test
    void theAdministratorDoesNotSubscribeToItsOwnPlatform() {
        assertEquals(Plan.FREE, billing.planOf(ADMIN));
    }

    private static Money price(BillingView view, PaymentMethod method, BillingPeriod period) {
        return view.offers().stream()
                .filter(offer -> offer.method() == method && offer.period() == period)
                .findFirst()
                .orElseThrow()
                .price();
    }
}
