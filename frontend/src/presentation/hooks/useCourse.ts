import { useCallback, useEffect, useState } from 'react';
import type { Course, CourseSection } from '@/domain/models/Course';
import { toAppError, type AppError } from '@/domain/errors/AppError';
import { useDependencies } from '../state/DependenciesContext';

export interface CourseState {
  course: Course | null;
  loading: boolean;
  error: AppError | null;
  /** Section dont l'état est en cours de bascule. */
  pending: string | null;
  toggle: (section: CourseSection) => Promise<void>;
  reload: () => Promise<void>;
}

export function useCourse(slug: string): CourseState {
  const { courses } = useDependencies();
  const [course, setCourse] = useState<Course | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<AppError | null>(null);
  const [pending, setPending] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setCourse(await courses.get.execute(slug));
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setLoading(false);
    }
  }, [courses.get, slug]);

  useEffect(() => {
    void reload();
  }, [reload]);

  const toggle = useCallback(
    async (section: CourseSection) => {
      setPending(section.slug);
      setError(null);
      try {
        setCourse(await courses.toggleSection.execute(slug, section.slug, section.completed));
      } catch (e) {
        setError(toAppError(e));
      } finally {
        setPending(null);
      }
    },
    [courses.toggleSection, slug],
  );

  return { course, loading, error, pending, toggle, reload };
}
