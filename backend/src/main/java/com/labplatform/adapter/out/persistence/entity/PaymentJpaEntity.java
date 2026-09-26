package com.labplatform.adapter.out.persistence.entity;

import com.labplatform.domain.billing.BillingPeriod;
import com.labplatform.domain.billing.PaymentMethod;
import com.labplatform.domain.billing.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Trace d'un paiement. Le montant est stocké en unités indivisibles avec sa
 * devise : jamais en nombre à virgule flottante, qui perdrait des centimes au
 * fil des additions.
 */
@Entity
@Table(name = "payment")
public class PaymentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference", nullable = false, unique = true, length = 32)
    private String reference;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false, length = 16)
    private BillingPeriod period;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 24)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PaymentStatus status;

    @Column(name = "provider_reference", length = 128)
    private String providerReference;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "settled_at")
    private Instant settledAt;

    protected PaymentJpaEntity() {
        // requis par JPA
    }

    public PaymentJpaEntity(Long id, String reference, Long userId, BillingPeriod period, long amountMinor,
                            String currency, PaymentMethod method, PaymentStatus status, String providerReference,
                            String failureReason, Instant createdAt, Instant settledAt) {
        this.id = id;
        this.reference = reference;
        this.userId = userId;
        this.period = period;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.method = method;
        this.status = status;
        this.providerReference = providerReference;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.settledAt = settledAt;
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

    public long getAmountMinor() {
        return amountMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSettledAt() {
        return settledAt;
    }
}
