import { useCallback, useEffect, useState } from 'react';
import { EMPTY_PROGRESS, type LeaderboardEntry, type PlayerProgress } from '@/domain/models/Progress';
import { toAppError, type AppError } from '@/domain/errors/AppError';
import { useDependencies } from '../state/DependenciesContext';

export interface ScoreboardState {
  progress: PlayerProgress;
  entries: LeaderboardEntry[];
  loading: boolean;
  error: AppError | null;
  reload: () => Promise<void>;
}

/** Progression personnelle et classement : les deux arrivent ensemble. */
export function useScoreboard(): ScoreboardState {
  const { scoreboard } = useDependencies();
  const [progress, setProgress] = useState<PlayerProgress>(EMPTY_PROGRESS);
  const [entries, setEntries] = useState<LeaderboardEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<AppError | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [mine, board] = await Promise.all([
        scoreboard.progress.execute(),
        scoreboard.leaderboard.execute(),
      ]);
      setProgress(mine);
      setEntries(board);
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setLoading(false);
    }
  }, [scoreboard.progress, scoreboard.leaderboard]);

  useEffect(() => {
    void reload();
  }, [reload]);

  return { progress, entries, loading, error, reload };
}
