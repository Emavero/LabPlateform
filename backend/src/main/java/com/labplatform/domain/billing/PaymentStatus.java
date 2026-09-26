package com.labplatform.domain.billing;

/** Cycle de vie d'un paiement. Seul {@link #SUCCEEDED} ouvre l'abonnement. */
public enum PaymentStatus {

    PENDING,
    SUCCEEDED,
    FAILED,
    CANCELLED;

    public boolean isSettled() {
        return this != PENDING;
    }
}
