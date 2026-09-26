package com.labplatform.domain.billing;

/**
 * État d'un abonnement. {@link #CANCELLED} n'est pas une coupure immédiate :
 * le mois déjà payé reste dû à l'abonné, seul le renouvellement s'arrête.
 */
public enum SubscriptionStatus {

    ACTIVE,
    CANCELLED,
    EXPIRED
}
