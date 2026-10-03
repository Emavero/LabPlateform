import { useCallback, useEffect, useState } from 'react';
import type { CourseSummary, LearningProgress, Track } from '@/domain/models/Course';
import { toAppError, type AppError } from '@/domain/errors/AppError';
import { useLanguageRefresh } from './useLanguageRefresh';
import { useDependencies } from '../state/DependenciesContext';

export interface CatalogueState {
  /** Tout le catalogue : le filtrage par filière et sous-domaine se fait sur place. */
  courses: CourseSummary[];
  /** Filières et leurs sous-domaines, tels que le serveur les décrit. */
  tracks: Track[];
  progresses: LearningProgress[];
  loading: boolean;
  error: AppError | null;
  reload: () => Promise<void>;
}

/**
 * Catalogue entier, filières comprises.
 * <p>
 * Tout est chargé d'un coup, puis filtré dans la page : les boutons de filière
 * et de sous-domaine répondent alors au clic, sans aller-retour. Le catalogue
 * d'une plateforme de formation se compte en dizaines de cours, pas en
 * milliers — le jour où ce ne sera plus vrai, c'est la pagination qu'il faudra
 * ajouter, pas un filtre côté serveur.
 */
export function useCatalogue(): CatalogueState {
  const { courses: academy } = useDependencies();
  const [courses, setCourses] = useState<CourseSummary[]>([]);
  const [tracks, setTracks] = useState<Track[]>([]);
  const [progresses, setProgresses] = useState<LearningProgress[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<AppError | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [list, allTracks, progress] = await Promise.all([
        academy.list.execute(),
        academy.tracks.execute(),
        academy.progress.execute(),
      ]);
      setCourses(list);
      setTracks(allTracks);
      setProgresses(progress);
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setLoading(false);
    }
  }, [academy.list, academy.progress, academy.tracks]);

  useEffect(() => {
    void reload();
  }, [reload]);
  useLanguageRefresh(reload);

  return { courses, tracks, progresses, loading, error, reload };
}
