package com.labplatform.domain.billing;

import java.time.Instant;
import java.util.Objects;

/**
 * Abonnement d'un compte, et seule autorité sur la question « ce compte a-t-il
 * accès au contenu Pro, maintenant ? ».
 * <p>
 * L'échéance n'est pas remise à zéro à chaque paiement : elle est prolongée
 * depuis la date d'expiration en cours quand celle-ci est future. Un abonné
 * qui renouvelle avec cinq jours d'avance ne perd donc pas ces cinq jours,
 * et deux notifications de paiement pour la même échéance ne créditent pas
 * deux fois (c'est le paiement, plus haut, qui est idempotent).
 * <p>
 * Un compte sans ligne en base est un compte gratuit : {@link #free(Long)}
 * évite au reste de l'application de manipuler des {@code null}.
 */
public class Subscription {

    private final Long id;
    private final Long userId;
    private Plan plan;
    private SubscriptionStatus status;
    private BillingPeriod period;
    private Instant startedAt;
    private Instant expiresAt;

    private Subscription(Long id, Long userId, Plan plan, SubscriptionStatus status, BillingPeriod period,
                         Instant startedAt, Instant expiresAt) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId, "userId");
        this.plan = Objects.requireNonNull(plan, "plan");
        this.status = Objects.requireNonNull(status, "status");
        this.period = period;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
    }

    /** Le compte tel qu'il est avant tout paiement : gratuit, sans échéance. */
    public static Subscription free(Long userId) {
        return new Subscription(null, userId, Plan.FREE, SubscriptionStatus.EXPIRED, null, null, null);
    }

    public static Subscription restore(Long id, Long userId, Plan plan, SubscriptionStatus status,
                                       BillingPeriod period, Instant startedAt, Instant expiresAt) {
        return new Subscription(Objects.requireNonNull(id, "id"), userId, plan, status, period, startedAt, expiresAt);
    }

    /**
     * Crédite la durée achetée. Prolonge l'échéance en cours si elle est
     * encore devant nous, part de maintenant sinon.
     */
    public void extend(BillingPeriod bought, Instant now) {
        Objects.requireNonNull(bought, "bought");
        Objects.requireNonNull(now, "now");
        Instant from = expiresAt != null && expiresAt.isAfter(now) ? expiresAt : now;
        if (startedAt == null || !isActiveAt(now)) {
            this.startedAt = now;
        }
        this.plan = Plan.PRO;
        this.status = SubscriptionStatus.ACTIVE;
        this.period = bought;
        this.expiresAt = bought.addTo(from);
    }

    /**
     * Arrête le renouvellement sans retirer ce qui est payé : l'accès court
     * jusqu'à l'échéance, c'est l'état qui change tout de suite.
     */
    public void cancel(Instant now) {
        Objects.requireNonNull(now, "now");
        this.status = isActiveAt(now) ? SubscriptionStatus.CANCELLED : SubscriptionStatus.EXPIRED;
    }

    /**
     * Accès Pro à cet instant. Un abonnement résilié mais non échu y donne
     * encore droit ; un abonnement « actif » dont l'échéance est passée, non :
     * l'état stocké n'est jamais cru sur parole face à la date.
     */
    public boolean isActiveAt(Instant now) {
        Objects.requireNonNull(now, "now");
        return plan.isPro()
                && status != SubscriptionStatus.EXPIRED
                && expiresAt != null
                && expiresAt.isAfter(now);
    }

    /** Plan effectif à cet instant, échéance comprise. */
    public Plan planAt(Instant now) {
        return isActiveAt(now) ? Plan.PRO : Plan.FREE;
    }

    /** État à afficher, échéance comprise : un abonnement échu se dit échu. */
    public SubscriptionStatus statusAt(Instant now) {
        return isActiveAt(now) ? status : SubscriptionStatus.EXPIRED;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Plan getPlan() {
        return plan;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public BillingPeriod getPeriod() {
        return period;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
