import type { Analytics } from '../models/Analytics';

export interface AnalyticsRepository {
  /** Indicateurs de la plateforme sur les `windowDays` derniers jours. */
  get(windowDays: number): Promise<Analytics>;
}
