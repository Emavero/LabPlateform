package com.labplatform.application.service;

import com.labplatform.application.port.in.billing.BillingView;
import com.labplatform.application.port.in.billing.CancelSubscriptionUseCase;
import com.labplatform.application.port.in.billing.CheckoutTicket;
import com.labplatform.application.port.in.billing.ConfirmPaymentUseCase;
import com.labplatform.application.port.in.billing.GetBillingUseCase;
import com.labplatform.application.port.in.billing.GetEffectivePlanUseCase;
import com.labplatform.application.port.in.billing.PaymentSummary;
import com.labplatform.application.port.in.billing.PlanOffer;
import com.labplatform.application.port.in.billing.StartCheckoutUseCase;
import com.labplatform.application.port.out.JournalPort;
import com.labplatform.application.port.out.PaymentGatewayPort;
import com.labplatform.application.port.out.PaymentRepositoryPort;
import com.labplatform.application.port.out.SecretGeneratorPort;
import com.labplatform.application.port.out.SubscriptionRepositoryPort;
import com.labplatform.application.port.out.TransactionPort;
import com.labplatform.domain.billing.BillingPeriod;
import com.labplatform.domain.billing.Money;
import com.labplatform.domain.billing.Payment;
import com.labplatform.domain.billing.PaymentMethod;
import com.labplatform.domain.billing.PaymentStatus;
import com.labplatform.domain.billing.Plan;
import com.labplatform.domain.billing.Subscription;
import com.labplatform.domain.billing.SubscriptionStatus;
import com.labplatform.domain.journal.JournalEvent;
import com.labplatform.domain.journal.JournalKind;
import com.labplatform.domain.shared.ConflictException;
import com.labplatform.domain.shared.InvalidInputException;
import com.labplatform.domain.shared.NotFoundException;
import com.labplatform.domain.shared.ServiceUnavailableException;
import com.labplatform.domain.user.Actor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Abonnements et paiements.
 * <p>
 * Trois principes tiennent ce service :
 * <ul>
 *   <li>L'appel au prestataire se fait <em>hors transaction</em>. Ouvrir une
 *       session de paiement passe par le réseau et peut durer : garder une
 *       transaction ouverte pendant ce temps immobiliserait une connexion à la
 *       base et un verrou sur la ligne de paiement.</li>
 *   <li>Le service ne décide jamais qu'un paiement a réussi. Il le demande au
 *       prestataire (retour du payeur) ou reçoit sa notification signée. Une
 *       requête du navigateur ne suffit donc pas à s'offrir un abonnement.</li>
 *   <li>Tout est idempotent : la même notification reçue deux fois ne crédite
 *       qu'une échéance, l'agrégat {@link Payment} s'en assure.</li>
 * </ul>
 */
public class BillingService implements GetBillingUseCase, GetEffectivePlanUseCase, StartCheckoutUseCase,
        ConfirmPaymentUseCase, CancelSubscriptionUseCase {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    /** Ce que l'abonné voit de son historique : au-delà, la page n'aide plus. */
    private static final int HISTORY_SIZE = 10;

    private final SubscriptionRepositoryPort subscriptions;
    private final PaymentRepositoryPort payments;
    private final PaymentGatewayPort gateway;
    private final SecretGeneratorPort secrets;
    private final JournalPort journal;
    private final TransactionPort transactions;
    private final Clock clock;
    private final BillingSettings settings;

    public BillingService(SubscriptionRepositoryPort subscriptions, PaymentRepositoryPort payments,
                          PaymentGatewayPort gateway, SecretGeneratorPort secrets, JournalPort journal,
                          TransactionPort transactions, Clock clock, BillingSettings settings) {
        this.subscriptions = subscriptions;
        this.payments = payments;
        this.gateway = gateway;
        this.secrets = secrets;
        this.journal = journal;
        this.transactions = transactions;
        this.clock = clock;
        this.settings = settings;
    }

    @Override
    public Plan planOf(Actor actor) {
        // Facturation coupée : la plateforme entière est ouverte, ce qui est le
        // mode attendu d'une installation de démonstration ou d'un lab interne.
        if (!settings.enabled()) {
            return Plan.PRO;
        }
        Instant now = clock.instant();
        return subscriptions.findByUser(actor.userId())
                .map(subscription -> subscription.planAt(now))
                .orElse(Plan.FREE);
    }

    @Override
    public BillingView billingOf(Actor actor) {
        Instant now = clock.instant();
        Subscription subscription = subscriptions.findByUser(actor.userId())
                .orElseGet(() -> Subscription.free(actor.userId()));
        List<PaymentSummary> history = payments.findByUser(actor.userId()).stream()
                .limit(HISTORY_SIZE)
                .map(BillingService::summarise)
                .toList();
        return new BillingView(
                settings.enabled() ? subscription.planAt(now) : Plan.PRO,
                subscription.statusAt(now),
                subscription.getExpiresAt(),
                subscription.statusAt(now) == SubscriptionStatus.ACTIVE,
                offers(),
                history);
    }

    @Override
    public CheckoutTicket startCheckout(Actor actor, PaymentMethod method, BillingPeriod period) {
        requireOpenForBusiness(method);
        Money amount = settings.pricesFor(method).priceOf(period);

        Payment pending = transactions.inTransaction(() -> payments.save(
                Payment.initiate(secrets.hexToken(), actor.userId(), period, amount, method, clock.instant())));

        PaymentGatewayPort.Checkout checkout;
        try {
            checkout = gateway.open(new PaymentGatewayPort.CheckoutRequest(pending.getReference(), amount, method,
                    "cyberMans Pro — " + period.displayName().toLowerCase(java.util.Locale.ROOT),
                    settings.successUrl() + "?reference=" + pending.getReference(),
                    settings.cancelUrl() + "?reference=" + pending.getReference()));
        } catch (RuntimeException failure) {
            // Le paiement reste tracé comme échoué : sans cela, la page
            // d'abonnement afficherait une tentative en cours indéfiniment.
            log.warn("Ouverture de la session de paiement refusée ({})", method, failure);
            transactions.inTransaction(() -> {
                Payment reloaded = require(pending.getReference());
                reloaded.fail("Prestataire indisponible", clock.instant());
                return payments.save(reloaded);
            });
            throw new ServiceUnavailableException("Le service de paiement est momentanément indisponible");
        }

        transactions.inTransaction(() -> {
            Payment reloaded = require(pending.getReference());
            reloaded.handedTo(checkout.providerReference());
            return payments.save(reloaded);
        });

        journal.record(JournalEvent.of(actor.userId(), JournalKind.CHECKOUT_STARTED, method.name(),
                period.name(), clock.instant()));
        return new CheckoutTicket(pending.getReference(), checkout.redirectUrl());
    }

    @Override
    public BillingView confirm(Actor actor, String reference) {
        Payment payment = payments.findByReference(normalise(reference))
                // Introuvable plutôt qu'interdit : sinon, essayer des
                // références au hasard révélerait celles qui existent.
                .filter(candidate -> candidate.isOwnedBy(actor.userId()))
                .orElseThrow(() -> new NotFoundException("Paiement introuvable"));

        if (payment.hasSucceeded()) {
            return billingOf(actor);
        }
        String providerReference = payment.getProviderReference()
                .orElseThrow(() -> new ConflictException("Ce paiement n'a jamais été ouvert chez le prestataire"));

        PaymentStatus observed = gateway.verify(payment.getMethod(), providerReference);
        settle(payment.getReference(), observed, null);
        return billingOf(actor);
    }

    @Override
    public void applyNotification(PaymentMethod method, String signature, String rawBody) {
        Optional<PaymentGatewayPort.Notification> parsed = gateway.readNotification(method, signature, rawBody);
        if (parsed.isEmpty()) {
            return;
        }
        PaymentGatewayPort.Notification notification = parsed.get();
        Optional<Payment> known = payments.findByReference(normalise(notification.reference()));
        if (known.isEmpty()) {
            // Notification signée mais inconnue : un autre environnement
            // partage peut-être la même clé. On l'ignore sans échouer.
            log.info("Notification de paiement ignorée, référence inconnue");
            return;
        }
        Payment payment = known.get();
        if (!matchesSession(payment, notification)) {
            log.warn("Notification de paiement rejetée : la session ne correspond pas à la référence");
            return;
        }
        settle(payment.getReference(), notification.status(), notification.reason());
    }

    @Override
    public BillingView cancel(Actor actor) {
        Instant now = clock.instant();
        transactions.inTransaction(() -> {
            Subscription subscription = subscriptions.findByUser(actor.userId())
                    .filter(candidate -> candidate.isActiveAt(now))
                    .orElseThrow(() -> new ConflictException("Aucun abonnement en cours"));
            subscription.cancel(now);
            return subscriptions.save(subscription);
        });
        journal.record(JournalEvent.of(actor.userId(), JournalKind.SUBSCRIPTION_CANCELLED, null, now));
        return billingOf(actor);
    }

    /**
     * Applique l'issue observée. Le crédit de l'abonnement et le changement
     * d'état du paiement sont dans la même transaction : on ne veut pas d'un
     * paiement encaissé sans abonnement, ni l'inverse.
     */
    private void settle(String reference, PaymentStatus observed, String reason) {
        Instant now = clock.instant();
        // Le journal est écrit après la transaction, jamais dedans : un
        // enregistrement qui survivrait à un rollback annoncerait un
        // abonnement que personne n'a.
        Settlement settlement = transactions.inTransaction(() -> {
            Payment payment = require(reference);
            switch (observed) {
                case SUCCEEDED -> {
                    if (!payment.succeed(now)) {
                        // Notification répétée : déjà réglé, rien à créditer.
                        return new Settlement(payment, false);
                    }
                    Subscription subscription = subscriptions.findByUser(payment.getUserId())
                            .orElseGet(() -> Subscription.free(payment.getUserId()));
                    subscription.extend(payment.getPeriod(), now);
                    subscriptions.save(subscription);
                }
                case FAILED -> payment.fail(reason, now);
                case CANCELLED -> payment.cancel(now);
                // Toujours en attente chez le prestataire : rien à écrire.
                case PENDING -> {
                    return new Settlement(payment, false);
                }
            }
            return new Settlement(payments.save(payment), true);
        });

        if (!settlement.applied()) {
            return;
        }
        Payment settled = settlement.payment();
        switch (observed) {
            case SUCCEEDED -> journal.record(JournalEvent.of(settled.getUserId(), JournalKind.SUBSCRIPTION_STARTED,
                    settled.getMethod().name(), settled.getPeriod().name(), now));
            case FAILED -> journal.record(JournalEvent.of(settled.getUserId(), JournalKind.PAYMENT_FAILED,
                    settled.getMethod().name(), reason, now));
            case PENDING, CANCELLED -> {
                // Un abandon ou une attente ne disent rien d'utile sur l'usage.
            }
        }
    }

    /** Ce que la transaction de règlement rapporte : l'état, et s'il a changé. */
    private record Settlement(Payment payment, boolean applied) {
    }

    /**
     * La notification porte-t-elle bien sur la session ouverte pour cette
     * référence ? Une signature valide prouve que le prestataire parle, pas
     * qu'il parle du bon paiement.
     */
    private static boolean matchesSession(Payment payment, PaymentGatewayPort.Notification notification) {
        return payment.getProviderReference()
                .map(known -> known.equals(notification.providerReference()))
                .orElse(true);
    }

    private List<PlanOffer> offers() {
        List<PlanOffer> offers = new ArrayList<>();
        for (PaymentMethod method : PaymentMethod.values()) {
            if (!settings.isEnabled(method)) {
                continue;
            }
            boolean available = gateway.supports(method);
            for (BillingPeriod period : BillingPeriod.values()) {
                offers.add(new PlanOffer(method, period, settings.pricesFor(method).priceOf(period), available));
            }
        }
        return offers;
    }

    private void requireOpenForBusiness(PaymentMethod method) {
        if (!settings.enabled()) {
            throw new ConflictException("La facturation est désactivée sur cette instance");
        }
        if (!settings.isEnabled(method) || !gateway.supports(method)) {
            throw new InvalidInputException("Ce moyen de paiement n'est pas proposé");
        }
    }

    private Payment require(String reference) {
        return payments.findByReference(reference)
                .orElseThrow(() -> new NotFoundException("Paiement introuvable"));
    }

    private static String normalise(String reference) {
        return reference == null ? "" : reference.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static PaymentSummary summarise(Payment payment) {
        return new PaymentSummary(payment.getReference(), payment.getAmount(), payment.getMethod(),
                payment.getStatus(), payment.getCreatedAt(), payment.getSettledAt().orElse(null));
    }
}
