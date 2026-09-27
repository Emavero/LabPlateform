import { useCallback, useEffect, useState } from 'react';
import { myWriteup, type Writeup, type WriteupDraft } from '@/domain/models/Writeup';
import { toAppError, type AppError } from '@/domain/errors/AppError';
import { useLanguageRefresh } from './useLanguageRefresh';
import { useDependencies } from '../state/DependenciesContext';

export interface WriteupsState {
  writeups: Writeup[];
  mine: Writeup | undefined;
  loading: boolean;
  error: AppError | null;
  saving: boolean;
  save: (draft: WriteupDraft) => Promise<boolean>;
  remove: () => Promise<void>;
  reload: () => Promise<void>;
}

/** Comptes rendus d'une machine : le sien, et ceux que la règle de lecture autorise. */
export function useWriteups(slug: string): WriteupsState {
  const { writeups: repository } = useDependencies();
  const [writeups, setWriteups] = useState<Writeup[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<AppError | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setWriteups(await repository.list.execute(slug));
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setLoading(false);
    }
  }, [repository.list, slug]);

  useEffect(() => {
    void reload();
  }, [reload]);
  useLanguageRefresh(reload);

  const save = useCallback(
    async (draft: WriteupDraft) => {
      setSaving(true);
      setError(null);
      try {
        await repository.save.execute(slug, draft);
        await reload();
        return true;
      } catch (e) {
        setError(toAppError(e));
        return false;
      } finally {
        setSaving(false);
      }
    },
    [repository.save, reload, slug],
  );

  const remove = useCallback(async () => {
    setError(null);
    try {
      await repository.remove.execute(slug);
      await reload();
    } catch (e) {
      setError(toAppError(e));
    }
  }, [repository.remove, reload, slug]);

  return { writeups, mine: myWriteup(writeups), loading, error, saving, save, remove, reload };
}
