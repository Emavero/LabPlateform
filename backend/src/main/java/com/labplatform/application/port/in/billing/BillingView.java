package com.labplatform.application.port.in.billing;

import com.labplatform.domain.billing.Plan;
import com.labplatform.domain.billing.SubscriptionStatus;

import java.time.Instant;
import java.util.List;

/**
 * Tout ce que la page d'abonnement a besoin de savoir : où en est le compte,
 * ce qui lui est proposé, et ce qu'il a déjà payé.
 *
 * @param plan      formule effective à l'instant de la lecture
 * @param expiresAt échéance, nulle pour un compte gratuit
 */
public record BillingView(Plan plan, SubscriptionStatus status, Instant expiresAt, boolean renewing,
                          List<PlanOffer> offers, List<PaymentSummary> payments) {
}
