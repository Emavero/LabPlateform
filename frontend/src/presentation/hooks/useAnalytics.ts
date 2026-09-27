import { useCallback, useEffect, useState } from 'react';
import type { Analytics } from '@/domain/models/Analytics';
import { toAppError, type AppError } from '@/domain/errors/AppError';
import { useDependencies } from '../state/DependenciesContext';

export interface AnalyticsState {
  analytics: Analytics | null;
  windowDays: number;
  loading: boolean;
  error: AppError | null;
  setWindow: (days: number) => void;
  reload: () => Promise<void>;
}

/** Indicateurs de la plateforme, sur une fenêtre que l'administrateur choisit. */
export function useAnalytics(initialWindow = 30): AnalyticsState {
  const { admin } = useDependencies();
  const [analytics, setAnalytics] = useState<Analytics | null>(null);
  const [windowDays, setWindowDays] = useState(initialWindow);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<AppError | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setAnalytics(await admin.analytics.execute(windowDays));
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setLoading(false);
    }
  }, [admin.analytics, windowDays]);

  useEffect(() => {
    void reload();
  }, [reload]);

  return { analytics, windowDays, loading, error, setWindow: setWindowDays, reload };
}
