import { useCallback, useEffect, useState } from 'react';
import { EMPTY_PROGRESS, type PlayerProgress } from '@/domain/models/Progress';
import { toAppError, type AppError } from '@/domain/errors/AppError';
import { useDependencies } from '../state/DependenciesContext';

/** Progression seule, pour les écrans qui n'ont pas besoin du classement. */
export function useProgress() {
  const { scoreboard } = useDependencies();
  const [progress, setProgress] = useState<PlayerProgress>(EMPTY_PROGRESS);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<AppError | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setProgress(await scoreboard.progress.execute());
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setLoading(false);
    }
  }, [scoreboard.progress]);

  useEffect(() => {
    void reload();
  }, [reload]);

  return { progress, loading, error, reload };
}
