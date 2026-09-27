import type { Money } from './Billing';

/** Profil d'usage d'un compte, tel que le serveur le classe. */
export type UsageProfileCode = 'HUNTER' | 'LEARNER' | 'BUILDER' | 'BROWSER' | 'DORMANT';
export type RecommendationSeverity = 'WARNING' | 'OPPORTUNITY' | 'INFO';

export interface AudienceMetrics {
  readonly users: number;
  readonly newUsers: number;
  readonly activeUsers: number;
  readonly dormantUsers: number;
  readonly proUsers: number;
  readonly activeRate: number;
  readonly conversionRate: number;
  /** Comptes déjà présents au début de la fenêtre. Nul : rien à mesurer. */
  readonly retentionCohort: number;
  readonly retentionRate: number;
}

export interface EngagementMetrics {
  readonly flagsValidated: number;
  readonly flagsRefused: number;
  readonly boxesPwned: number;
  readonly targetsSpawned: number;
  readonly coursesViewed: number;
  readonly sectionsCompleted: number;
  readonly quizPassed: number;
  readonly quizFailed: number;
  readonly writeupsPublished: number;
  readonly flagRefusalRate: number;
  readonly quizPassRate: number;
}

export interface RevenueMetrics {
  /** Un total par devise : des francs CFA et des euros ne s'additionnent pas. */
  readonly collected: readonly Money[];
  readonly paymentsSucceeded: number;
  readonly paymentsFailed: number;
  readonly failureRate: number;
  readonly checkoutsStarted: number;
  readonly checkoutCompletionRate: number;
}

/** Les mêmes chiffres sur la période précédente, de même longueur. */
export interface WindowComparison {
  readonly newUsers: number;
  readonly activeUsers: number;
  readonly flagsValidated: number;
  readonly sectionsCompleted: number;
  readonly subscriptionsStarted: number;
}

export interface UsageSegment {
  readonly profile: UsageProfileCode;
  readonly profileName: string;
  /** Ce qu'il y a à faire pour cette classe de comptes. */
  readonly advice: string;
  readonly accounts: number;
  readonly share: number;
}

export interface ContentInsight {
  readonly code: string;
  readonly title: string;
  readonly unit: string;
  readonly entries: readonly { subject: string; count: number }[];
}

export interface Recommendation {
  readonly code: string;
  readonly severity: RecommendationSeverity;
  readonly severityName: string;
  readonly title: string;
  readonly advice: string;
  /** Le chiffre qui motive le conseil : sans lui, il ne se vérifie pas. */
  readonly evidence: string;
  readonly subject: string | null;
}

export interface Analytics {
  readonly generatedAt: Date;
  readonly windowDays: number;
  readonly catalogue: Readonly<Record<string, number>>;
  readonly audience: AudienceMetrics;
  readonly engagement: EngagementMetrics;
  readonly revenue: RevenueMetrics;
  readonly previous: WindowComparison;
  readonly segments: readonly UsageSegment[];
  readonly insights: readonly ContentInsight[];
  readonly recommendations: readonly Recommendation[];
}

/** Fenêtres proposées : la semaine pour réagir, le mois pour décider, le trimestre pour la tendance. */
export const WINDOWS: readonly number[] = [7, 30, 90];

/**
 * Variation en pourcentage entre deux périodes, arrondie.
 * <p>
 * Partir de zéro n'est pas une hausse infinie : on rend `null`, et l'interface
 * affiche « nouveau » plutôt qu'un pourcentage qui ne veut rien dire.
 */
export function deltaPercent(current: number, previous: number): number | null {
  if (previous === 0) return current === 0 ? 0 : null;
  return Math.round(((current - previous) / previous) * 100);
}

/**
 * Nombre abrégé pour une tuile : 1 284 → 1,3 k.
 * <p>
 * Le séparateur décimal vient de la locale : en français on écrit « 1,3 k », et
 * un point à cet endroit se lit comme une faute.
 */
export function compact(value: number, locale = 'fr-FR'): string {
  const format = (amount: number) =>
    new Intl.NumberFormat(locale, { maximumFractionDigits: 1 }).format(amount);
  if (Math.abs(value) < 1000) return format(value);
  if (Math.abs(value) < 1_000_000) return `${format(value / 1000)} k`;
  return `${format(value / 1_000_000)} M`;
}

/**
 * Largeur d'une barre en pourcentage de la plus grande valeur du classement.
 * Comparer à la plus grande — et non au total — garde les écarts lisibles quand
 * une entrée écrase les autres.
 */
export function barWidth(count: number, entries: readonly { count: number }[]): number {
  const largest = entries.reduce((max, entry) => Math.max(max, entry.count), 0);
  return largest === 0 ? 0 : Math.round((count / largest) * 100);
}

export function insightOf(analytics: Analytics, code: string): ContentInsight | undefined {
  return analytics.insights.find((insight) => insight.code === code);
}

/** Classements qui ont au moins une entrée : les vides n'apprennent rien. */
export function filledInsights(analytics: Analytics): ContentInsight[] {
  return analytics.insights.filter((insight) => insight.entries.length > 0);
}
