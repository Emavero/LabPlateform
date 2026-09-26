import { useCallback, useEffect, useState } from 'react';
import type { JournalLine } from '@/domain/models/Journal';
import { toAppError, type AppError } from '@/domain/errors/AppError';
import { useDependencies } from '../state/DependenciesContext';

export interface JournalState {
  lines: JournalLine[];
  loading: boolean;
  error: AppError | null;
  reload: () => Promise<void>;
}

/**
 * Journal d'activité.
 *
 * @param scope `mine` pour le compte connecté, `platform` pour toute la
 *              plateforme — que le serveur refuse à un non-administrateur.
 */
export function useJournal(scope: 'mine' | 'platform' = 'mine', limit = 100): JournalState {
  const { journal } = useDependencies();
  const [lines, setLines] = useState<JournalLine[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<AppError | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setLines(scope === 'mine' ? await journal.mine.execute(limit) : await journal.platform.execute(limit));
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setLoading(false);
    }
  }, [journal.mine, journal.platform, scope, limit]);

  useEffect(() => {
    void reload();
  }, [reload]);

  return { lines, loading, error, reload };
}
