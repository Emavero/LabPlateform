import { useCallback, useEffect, useMemo, useState } from 'react';
import { matchesFilter, type Box, type BoxFilter } from '@/domain/models/Box';
import { toAppError, type AppError } from '@/domain/errors/AppError';
import { useDependencies } from '../state/DependenciesContext';

export interface BoxesState {
  boxes: Box[];
  /** Catalogue restreint au filtre courant. */
  visible: Box[];
  filter: BoxFilter;
  setFilter: (filter: BoxFilter) => void;
  loading: boolean;
  error: AppError | null;
  reload: () => Promise<void>;
}

export function useBoxes(): BoxesState {
  const { boxes: catalogue } = useDependencies();
  const [boxes, setBoxes] = useState<Box[]>([]);
  const [filter, setFilter] = useState<BoxFilter>('ALL');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<AppError | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setBoxes(await catalogue.list.execute());
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setLoading(false);
    }
  }, [catalogue.list]);

  useEffect(() => {
    void reload();
  }, [reload]);

  const visible = useMemo(() => boxes.filter((box) => matchesFilter(box, filter)), [boxes, filter]);

  return { boxes, visible, filter, setFilter, loading, error, reload };
}
