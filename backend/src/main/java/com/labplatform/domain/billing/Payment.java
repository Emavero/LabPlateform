package com.labplatform.domain.billing;

import com.labplatform.domain.shared.ConflictException;
import com.labplatform.domain.shared.InvalidInputException;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Tentative de paiement d'une échéance d'abonnement.
 * <p>
 * L'agrégat tient la règle la plus importante de la facturation : un paiement
 * ne se règle qu'une fois. Les prestataires renvoient la même notification
 * plusieurs fois (reprise après incident, nouvelle tentative sur un délai
 * dépassé) ; {@link #succeed} le sait et ne crédite donc rien la deuxième
 * fois, au lieu d'ajouter un mois par notification reçue.
 * <p>
 * La référence est la nôtre, tirée au hasard : elle circule dans les URL de
 * retour et chez le prestataire, donc elle ne doit pas être devinable — sans
 * quoi on pourrait tenter de confirmer le paiement d'un autre.
 */
public class Payment {

    private static final Pattern REFERENCE_FORMAT = Pattern.compile("^[0-9a-f]{32}$");
    private static final int MAX_PROVIDER_REFERENCE_LENGTH = 128;

    private final Long id;
    private final String reference;
    private final Long userId;
    private final BillingPeriod period;
    private final Money amount;
    private final PaymentMethod method;
    private PaymentStatus status;
    private String providerReference;
    private String failureReason;
    private final Instant createdAt;
    private Instant settledAt;

    private Payment(Long id, String reference, Long userId, BillingPeriod period, Money amount, PaymentMethod method,
                    PaymentStatus status, String providerReference, String failureReason, Instant createdAt,
                    Instant settledAt) {
        this.id = id;
        this.reference = requireReference(reference);
        this.userId = Objects.requireNonNull(userId, "userId");
        this.period = Objects.requireNonNull(period, "period");
        this.amount = requireAmount(amount);
        this.method = Objects.requireNonNull(method, "method");
        this.status = Objects.requireNonNull(status, "status");
        this.providerReference = providerReference;
        this.failureReason = failureReason;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.settledAt = settledAt;
    }

    public static Payment initiate(String reference, Long userId, BillingPeriod period, Money amount,
                                   PaymentMethod method, Instant now) {
        return new Payment(null, reference, userId, period, amount, method, PaymentStatus.PENDING, null, null, now,
                null);
    }

    public static Payment restore(Long id, String reference, Long userId, BillingPeriod period, Money amount,
                                  PaymentMethod method, PaymentStatus status, String providerReference,
                                  String failureReason, Instant createdAt, Instant settledAt) {
        return new Payment(Objects.requireNonNull(id, "id"), reference, userId, period, amount, method, status,
                providerReference, failureReason, createdAt, settledAt);
    }

    /** Identifiant rendu par le prestataire une fois la session de paiement ouverte. */
    public void handedTo(String providerReference) {
        this.providerReference = requireProviderReference(providerReference);
    }

    /**
     * Encaissement confirmé.
     *
     * @return vrai si c'est la première confirmation, donc s'il y a lieu de
     *         créditer l'abonnement ; faux si le paiement était déjà réglé.
     */
    public boolean succeed(Instant now) {
        if (status == PaymentStatus.SUCCEEDED) {
            return false;
        }
        requireOpen();
        this.status = PaymentStatus.SUCCEEDED;
        this.settledAt = Objects.requireNonNull(now, "now");
        return true;
    }

    public void fail(String reason, Instant now) {
        if (status == PaymentStatus.FAILED) {
            return;
        }
        requireOpen();
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason == null || reason.isBlank() ? null : reason.trim();
        this.settledAt = Objects.requireNonNull(now, "now");
    }

    /** Abandonné par le payeur avant d'aller au bout. */
    public void cancel(Instant now) {
        if (status == PaymentStatus.CANCELLED) {
            return;
        }
        requireOpen();
        this.status = PaymentStatus.CANCELLED;
        this.settledAt = Objects.requireNonNull(now, "now");
    }

    public boolean isOwnedBy(Long candidate) {
        return userId.equals(candidate);
    }

    public boolean hasSucceeded() {
        return status == PaymentStatus.SUCCEEDED;
    }

    /**
     * Un paiement déjà réglé ne repart pas dans l'autre sens : un encaissement
     * ne devient pas un échec parce qu'une notification arrive en retard, et
     * un échec ne se transforme pas en encaissement.
     */
    private void requireOpen() {
        if (status.isSettled()) {
            throw new ConflictException("Ce paiement est déjà clos");
        }
    }

    private static String requireReference(String reference) {
        if (reference == null || !REFERENCE_FORMAT.matcher(reference).matches()) {
            throw new InvalidInputException("Référence de paiement invalide");
        }
        return reference;
    }

    private static Money requireAmount(Money amount) {
        Objects.requireNonNull(amount, "amount");
        if (amount.isZero()) {
            throw new InvalidInputException("Le montant à payer est nul");
        }
        return amount;
    }

    private static String requireProviderReference(String providerReference) {
        if (providerReference == null || providerReference.isBlank()) {
            throw new InvalidInputException("Le prestataire n'a pas renvoyé de référence");
        }
        String trimmed = providerReference.trim();
        if (trimmed.length() > MAX_PROVIDER_REFERENCE_LENGTH) {
            throw new InvalidInputException("Référence du prestataire trop longue");
        }
        return trimmed;
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public Long getUserId() {
        return userId;
    }

    public BillingPeriod getPeriod() {
        return period;
    }

    public Money getAmount() {
        return amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Optional<String> getProviderReference() {
        return Optional.ofNullable(providerReference);
    }

    public Optional<String> getFailureReason() {
        return Optional.ofNullable(failureReason);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Optional<Instant> getSettledAt() {
        return Optional.ofNullable(settledAt);
    }
}
