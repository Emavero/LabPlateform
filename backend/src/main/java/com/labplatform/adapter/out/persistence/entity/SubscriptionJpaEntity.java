package com.labplatform.adapter.out.persistence.entity;

import com.labplatform.domain.billing.BillingPeriod;
import com.labplatform.domain.billing.Plan;
import com.labplatform.domain.billing.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Une ligne par compte abonné. Un compte gratuit n'en a pas. */
@Entity
@Table(name = "subscription")
public class SubscriptionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, length = 16)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private SubscriptionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "period", length = 16)
    private BillingPeriod period;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected SubscriptionJpaEntity() {
        // requis par JPA
    }

    public SubscriptionJpaEntity(Long id, Long userId, Plan plan, SubscriptionStatus status, BillingPeriod period,
                                 Instant startedAt, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.plan = plan;
        this.status = status;
        this.period = period;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
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
