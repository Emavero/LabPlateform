import type { Billing, BillingPeriod, Checkout, PaymentMethod } from '../models/Billing';

export interface BillingRepository {
  get(): Promise<Billing>;
  /** Ouvre une session de paiement et rend l'adresse où envoyer le payeur. */
  startCheckout(method: PaymentMethod, period: BillingPeriod): Promise<Checkout>;
  /** Signale le retour du payeur ; c'est le serveur qui interroge le prestataire. */
  confirm(reference: string): Promise<Billing>;
  cancel(): Promise<Billing>;
}
