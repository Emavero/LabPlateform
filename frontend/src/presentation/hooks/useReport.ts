import { useCallback, useEffect, useState } from 'react';
import { toAppError, type AppError } from '@/domain/errors/AppError';
import type { ActivityReport } from '@/domain/models/Report';
import { useDependencies } from '../state/DependenciesContext';

export interface ReportState {
  report: ActivityReport | null;
  days: number;
  loading: boolean;
  error: AppError | null;
  setDays: (days: number) => void;
  reload: () => Promise<void>;
}

/**
 * Rapport d'activité du compte connecté.
 * <p>
 * Changer de période recharge : le rapport est calculé côté serveur, et le
 * recalculer là-bas évite d'avoir deux façons de compter — une par écran — qui
 * finiraient par ne plus dire la même chose.
 */
export function useReport(initialDays = 30): ReportState {
  const { reports } = useDependencies();
  const [report, setReport] = useState<ActivityReport | null>(null);
  const [days, setDays] = useState(initialDays);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<AppError | null>(null);

  const load = useCallback(
    async (window: number) => {
      setLoading(true);
      setError(null);
      try {
        setReport(await reports.activity.execute(window));
      } catch (e) {
        setError(toAppError(e));
      } finally {
        setLoading(false);
      }
    },
    [reports.activity],
  );

  useEffect(() => {
    void load(days);
  }, [load, days]);

  return { report, days, loading, error, setDays, reload: () => load(days) };
}
