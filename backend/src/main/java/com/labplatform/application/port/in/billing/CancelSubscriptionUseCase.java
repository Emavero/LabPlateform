package com.labplatform.application.port.in.billing;

import com.labplatform.domain.user.Actor;

public interface CancelSubscriptionUseCase {

    /** Arrête le renouvellement. L'accès court jusqu'à l'échéance déjà payée. */
    BillingView cancel(Actor actor);
}
