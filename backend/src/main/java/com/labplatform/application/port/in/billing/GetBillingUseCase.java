package com.labplatform.application.port.in.billing;

import com.labplatform.domain.user.Actor;

public interface GetBillingUseCase {

    BillingView billingOf(Actor actor);
}
