import { describe, expect, it } from 'vitest';
import { daysLeft, formatMoney, methodsOf, yearlySavings, type Billing, type PlanOffer } from './Billing';

const offers: PlanOffer[] = [
  {
    method: 'WAVE',
    methodName: 'Wave',
    period: 'MONTHLY',
    periodName: 'Mensuel',
    price: { minorUnits: 5000, amount: '5000', currency: 'XOF' },
    available: true,
  },
  {
    method: 'WAVE',
    methodName: 'Wave',
    period: 'YEARLY',
    periodName: 'Annuel',
    price: { minorUnits: 50000, amount: '50000', currency: 'XOF' },
    available: true,
  },
  {
    method: 'CARD',
    methodName: 'Carte bancaire',
    period: 'MONTHLY',
    periodName: 'Mensuel',
    price: { minorUnits: 800, amount: '8.00', currency: 'EUR' },
    available: false,
  },
];

describe('Billing', () => {
  it('écrit un montant selon les usages de sa devise', () => {
    // Le franc CFA n'a pas de centimes, l'euro en a deux : c'est Intl qui le sait.
    expect(formatMoney({ minorUnits: 5000, amount: '5000', currency: 'XOF' }, 'fr-FR')).toContain('5');
    expect(formatMoney({ minorUnits: 800, amount: '8.00', currency: 'EUR' }, 'fr-FR')).toContain('8,00');
  });

  it('annonce l’économie de la formule annuelle', () => {
    // Douze mois à 5 000 font 60 000 ; l'année à 50 000 en économise un sixième.
    expect(yearlySavings(offers, 'WAVE')).toBe(17);
  });

  it('n’annonce rien quand l’année ne fait rien économiser', () => {
    const flat: PlanOffer[] = [
      { ...offers[0] },
      { ...offers[1], price: { minorUnits: 60000, amount: '60000', currency: 'XOF' } },
    ];
    expect(yearlySavings(flat, 'WAVE')).toBe(0);
  });

  it('liste chaque moyen de paiement une seule fois', () => {
    expect(methodsOf(offers)).toEqual(['WAVE', 'CARD']);
  });

  it('compte les jours restants sans jamais descendre sous zéro', () => {
    const now = new Date('2026-01-15T10:00:00Z');
    const base: Billing = {
      plan: 'PRO',
      planName: 'Pro',
      status: 'ACTIVE',
      expiresAt: new Date('2026-01-25T10:00:00Z'),
      pro: true,
      renewing: true,
      offers,
      payments: [],
    };

    expect(daysLeft(base, now)).toBe(10);
    expect(daysLeft({ ...base, expiresAt: new Date('2026-01-01T10:00:00Z') }, now)).toBe(0);
    expect(daysLeft({ ...base, expiresAt: null }, now)).toBe(0);
  });
});
