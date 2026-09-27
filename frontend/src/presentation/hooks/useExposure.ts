import { useCallback, useEffect, useState } from 'react';
import { toAppError, type AppError } from '@/domain/errors/AppError';
import type { LabExposure } from '@/domain/models/Exposure';
import { useLanguageRefresh } from './useLanguageRefresh';
import { useDependencies } from '../state/DependenciesContext';

export interface ExposureState {
  exposure: LabExposure | null;
  loading: boolean;
  error: AppError | null;
  reload: () => Promise<void>;
}

/**
 * Surface d'attaque du lab.
 * <p>
 * L'analyse et les chemins viennent d'un seul appel : ils sortent du même
 * calcul, et deux requêtes le feraient deux fois.
 */
export function useExposure(): ExposureState {
  const { exposure: useCases } = useDependencies();
  const [exposure, setExposure] = useState<LabExposure | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<AppError | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setExposure(await useCases.get.execute());
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setLoading(false);
    }
  }, [useCases.get]);

  useEffect(() => {
    void reload();
  }, [reload]);
  useLanguageRefresh(reload);

  return { exposure, loading, error, reload };
}
