package com.labplatform.application.port.in.analytics;

import com.labplatform.domain.billing.Money;

import java.util.List;

/**
 * Ce que la plateforme encaisse sur la fenêtre.
 * <p>
 * Le chiffre d'affaires est une liste, une entrée par devise, et non un total :
 * additionner des francs CFA et des euros donnerait un nombre qui ne veut rien
 * dire. C'est le prix à payer pour encaisser dans deux zones à la fois.
 *
 * @param failureRate part des paiements refusés, en pourcentage
 */
public record RevenueMetrics(List<Money> collected, long paymentsSucceeded, long paymentsFailed, int failureRate,
                             long checkoutsStarted, int checkoutCompletionRate) {
}
