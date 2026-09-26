import { useCallback, useEffect, useState } from 'react';
import type { CourseSummary, LearningProgress } from '@/domain/models/Course';
import { toAppError, type AppError } from '@/domain/errors/AppError';
import { useDependencies } from '../state/DependenciesContext';

export interface CoursesState {
  courses: CourseSummary[];
  /** Avancement de la filière affichée, absent tant qu'elle n'est pas chargée. */
  progress: LearningProgress | null;
  loading: boolean;
  error: AppError | null;
  reload: () => Promise<void>;
}

/** Cours d'une filière, désignée par son slug (« forensique », « defense »). */
export function useCourses(trackSlug: string): CoursesState {
  const { courses: academy } = useDependencies();
  const [courses, setCourses] = useState<CourseSummary[]>([]);
  const [progress, setProgress] = useState<LearningProgress | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<AppError | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [list, tracks] = await Promise.all([
        academy.list.execute(trackSlug),
        academy.progress.execute(),
      ]);
      setCourses(list);
      setProgress(tracks.find((entry) => entry.trackSlug === trackSlug) ?? null);
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setLoading(false);
    }
  }, [academy.list, academy.progress, trackSlug]);

  useEffect(() => {
    void reload();
  }, [reload]);

  return { courses, progress, loading, error, reload };
}
