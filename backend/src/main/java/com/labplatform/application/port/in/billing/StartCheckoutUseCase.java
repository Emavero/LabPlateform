package com.labplatform.application.port.in.billing;

import com.labplatform.domain.billing.BillingPeriod;
import com.labplatform.domain.billing.PaymentMethod;
import com.labplatform.domain.user.Actor;

public interface StartCheckoutUseCase {

    CheckoutTicket startCheckout(Actor actor, PaymentMethod method, BillingPeriod period);
}
