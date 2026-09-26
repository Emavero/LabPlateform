/** Formule d'un compte : le catalogue se consulte en Découverte, il s'attaque en Pro. */
export type Plan = 'FREE' | 'PRO';
export type SubscriptionStatus = 'ACTIVE' | 'CANCELLED' | 'EXPIRED';
export type BillingPeriod = 'MONTHLY' | 'YEARLY';
export type PaymentMethod = 'CARD' | 'WAVE' | 'ORANGE_MONEY';
export type PaymentStatus = 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED';

/**
 * Montant tel que le serveur l'envoie : la valeur décimale pour l'afficher,
 * les unités indivisibles pour le comparer. Le client n'a donc jamais à savoir
 * combien de décimales a une devise — le franc CFA n'en a aucune.
 */
export interface Money {
  readonly minorUnits: number;
  readonly amount: string;
  readonly currency: string;
}

export interface PlanOffer {
  readonly method: PaymentMethod;
  readonly methodName: string;
  readonly period: BillingPeriod;
  readonly periodName: string;
  readonly price: Money;
  /** Faux quand le moyen est proposé mais non configuré côté serveur. */
  readonly available: boolean;
}

export interface PaymentRecord {
  readonly reference: string;
  readonly amount: Money;
  readonly method: PaymentMethod;
  readonly methodName: string;
  readonly status: PaymentStatus;
  readonly createdAt: Date;
  readonly settledAt: Date | null;
}

export interface Billing {
  readonly plan: Plan;
  readonly planName: string;
  readonly status: SubscriptionStatus;
  readonly expiresAt: Date | null;
  readonly pro: boolean;
  /** Vrai tant que l'abonnement se renouvelle ; faux dès qu'il est résilié. */
  readonly renewing: boolean;
  readonly offers: readonly PlanOffer[];
  readonly payments: readonly PaymentRecord[];
}

export interface Checkout {
  readonly reference: string;
  readonly redirectUrl: string;
}

/** Ordre d'affichage : la formule la plus engageante en second. */
export const PERIOD_ORDER: readonly BillingPeriod[] = ['MONTHLY', 'YEARLY'];

/**
 * Montant écrit pour un humain. `Intl` connaît les conventions de chaque
 * devise, y compris qu'un franc CFA ne s'écrit pas avec des centimes.
 */
export function formatMoney(money: Money, locale = 'fr-FR'): string {
  return new Intl.NumberFormat(locale, { style: 'currency', currency: money.currency }).format(
    Number(money.amount),
  );
}

/**
 * Économie réalisée en payant à l'année, en pourcentage entier. Nulle quand
 * l'année coûte douze mois : il n'y a alors rien à annoncer.
 */
export function yearlySavings(offers: readonly PlanOffer[], method: PaymentMethod): number {
  const monthly = offers.find((offer) => offer.method === method && offer.period === 'MONTHLY');
  const yearly = offers.find((offer) => offer.method === method && offer.period === 'YEARLY');
  if (!monthly || !yearly || monthly.price.minorUnits === 0) return 0;
  const twelveMonths = monthly.price.minorUnits * 12;
  if (yearly.price.minorUnits >= twelveMonths) return 0;
  return Math.round(((twelveMonths - yearly.price.minorUnits) / twelveMonths) * 100);
}

/** Jours restants avant l'échéance, jamais négatif. */
export function daysLeft(billing: Billing, now: Date = new Date()): number {
  if (!billing.expiresAt) return 0;
  return Math.max(0, Math.ceil((billing.expiresAt.getTime() - now.getTime()) / 86_400_000));
}

export function methodsOf(offers: readonly PlanOffer[]): PaymentMethod[] {
  return [...new Set(offers.map((offer) => offer.method))];
}
