import { useCallback, useEffect, useState } from 'react';
import type { LearningProgress } from '@/domain/models/Course';
import type { Achievement, ActivityEntry } from '@/domain/models/Profile';
import { EMPTY_PROGRESS, type PlayerProgress } from '@/domain/models/Progress';
import { toAppError, type AppError } from '@/domain/errors/AppError';
import { useDependencies } from '../state/DependenciesContext';

export interface ProfileState {
  progress: PlayerProgress;
  achievements: Achievement[];
  activity: ActivityEntry[];
  learning: LearningProgress[];
  loading: boolean;
  error: AppError | null;
  reload: () => Promise<void>;
}

/** Tout ce que la page Profil affiche, en une seule vague de requêtes. */
export function useProfile(): ProfileState {
  const { profile, scoreboard, courses } = useDependencies();
  const [progress, setProgress] = useState<PlayerProgress>(EMPTY_PROGRESS);
  const [achievements, setAchievements] = useState<Achievement[]>([]);
  const [activity, setActivity] = useState<ActivityEntry[]>([]);
  const [learning, setLearning] = useState<LearningProgress[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<AppError | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [mine, earned, recent, tracks] = await Promise.all([
        scoreboard.progress.execute(),
        profile.achievements.execute(),
        profile.activity.execute(),
        courses.progress.execute(),
      ]);
      setProgress(mine);
      setAchievements(earned);
      setActivity(recent);
      setLearning(tracks);
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setLoading(false);
    }
  }, [scoreboard.progress, profile.achievements, profile.activity, courses.progress]);

  useEffect(() => {
    void reload();
  }, [reload]);

  return { progress, achievements, activity, learning, loading, error, reload };
}
