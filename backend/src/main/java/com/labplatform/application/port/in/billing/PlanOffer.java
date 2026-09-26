package com.labplatform.application.port.in.billing;

import com.labplatform.domain.billing.BillingPeriod;
import com.labplatform.domain.billing.Money;
import com.labplatform.domain.billing.PaymentMethod;

/**
 * Une ligne de l'offre : ce que coûte cette durée avec ce moyen de paiement.
 * Le tarif dépend du moyen choisi parce que les devises ne sont pas les mêmes
 * partout — une carte se débite en euros, Wave en francs CFA.
 */
public record PlanOffer(PaymentMethod method, BillingPeriod period, Money price, boolean available) {
}
