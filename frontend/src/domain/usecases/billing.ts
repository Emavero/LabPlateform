import type { Billing, BillingPeriod, Checkout, PaymentMethod } from '../models/Billing';
import { AppError } from '../errors/AppError';
import type { BillingRepository } from '../repositories/BillingRepository';

export class GetBillingUseCase {
  constructor(private readonly billing: BillingRepository) {}

  execute(): Promise<Billing> {
    return this.billing.get();
  }
}

/**
 * Ouvre le paiement. Le moyen choisi doit être proposé <em>et</em> disponible :
 * un bouton grisé ne doit pas pouvoir être forcé, et le serveur refuserait de
 * toute façon.
 */
export class StartCheckoutUseCase {
  constructor(private readonly billing: BillingRepository) {}

  execute(method: PaymentMethod, period: BillingPeriod): Promise<Checkout> {
    return this.billing.startCheckout(method, period);
  }
}

/**
 * Confirme au retour du prestataire. Une référence absente ou mal formée ne
 * part pas sur le réseau : elle vient de l'URL, donc de l'extérieur.
 */
export class ConfirmPaymentUseCase {
  private static readonly REFERENCE = /^[0-9a-f]{32}$/;

  constructor(private readonly billing: BillingRepository) {}

  execute(reference: string): Promise<Billing> {
    const trimmed = reference.trim().toLowerCase();
    if (!ConfirmPaymentUseCase.REFERENCE.test(trimmed)) {
      return Promise.reject(AppError.validation({ reference: 'Référence de paiement invalide.' }));
    }
    return this.billing.confirm(trimmed);
  }
}

export class CancelSubscriptionUseCase {
  constructor(private readonly billing: BillingRepository) {}

  execute(): Promise<Billing> {
    return this.billing.cancel();
  }
}
