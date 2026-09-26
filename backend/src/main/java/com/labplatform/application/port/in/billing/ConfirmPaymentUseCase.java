package com.labplatform.application.port.in.billing;

import com.labplatform.domain.billing.PaymentMethod;
import com.labplatform.domain.user.Actor;

public interface ConfirmPaymentUseCase {

    /**
     * Relit l'état du paiement chez le prestataire au retour du payeur. Ne
     * croit pas le navigateur : c'est le prestataire qui dit si c'est payé.
     */
    BillingView confirm(Actor actor, String reference);

    /**
     * Applique une notification du prestataire. Idempotent : la même
     * notification reçue deux fois ne crédite qu'une échéance.
     */
    void applyNotification(PaymentMethod method, String signature, String rawBody);
}
