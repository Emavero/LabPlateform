import type { ActivityReport } from '../models/Report';

export interface ReportRepository {
  /** Rapport du compte connecté sur les derniers jours demandés. */
  activity(days: number): Promise<ActivityReport>;
}
