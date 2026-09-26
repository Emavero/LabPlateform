package com.labplatform.application.port.in.billing;

import com.labplatform.domain.billing.Money;
import com.labplatform.domain.billing.PaymentMethod;
import com.labplatform.domain.billing.PaymentStatus;

import java.time.Instant;

/** Ligne de l'historique de paiement montrée à l'abonné. */
public record PaymentSummary(String reference, Money amount, PaymentMethod method, PaymentStatus status,
                             Instant createdAt, Instant settledAt) {
}
