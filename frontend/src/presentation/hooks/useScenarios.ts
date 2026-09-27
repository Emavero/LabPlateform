import { useCallback, useEffect, useState } from 'react';
import { toAppError, type AppError } from '@/domain/errors/AppError';
import type { Scenario, ScenarioDraft } from '@/domain/models/Scenario';
import { useDependencies } from '../state/DependenciesContext';

export interface ScenariosState {
  scenarios: Scenario[];
  loading: boolean;
  error: AppError | null;
  reload: () => Promise<void>;
}

/** Scénarios publiés, avec l'avancement du compte connecté. */
export function useScenarios(): ScenariosState {
  const { scenarios: useCases } = useDependencies();
  const [scenarios, setScenarios] = useState<Scenario[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<AppError | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setScenarios(await useCases.listPublished.execute());
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setLoading(false);
    }
  }, [useCases.listPublished]);

  useEffect(() => {
    void reload();
  }, [reload]);

  return { scenarios, loading, error, reload };
}

export interface AdminScenariosState {
  scenarios: Scenario[];
  loading: boolean;
  busy: boolean;
  error: AppError | null;
  save: (draft: ScenarioDraft, slug?: string) => Promise<string | null>;
  remove: (slug: string) => Promise<void>;
  reload: () => Promise<void>;
}

/**
 * Scénarios vus de l'administration, brouillons compris.
 * <p>
 * Chaque écriture recharge la liste : l'ordre suit la dernière modification, et
 * reclasser la liste dans le navigateur reviendrait à réimplémenter une règle
 * qui appartient au serveur.
 */
export function useAdminScenarios(): AdminScenariosState {
  const { scenarios: useCases } = useDependencies();
  const [scenarios, setScenarios] = useState<Scenario[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<AppError | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setScenarios(await useCases.listAll.execute());
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setLoading(false);
    }
  }, [useCases.listAll]);

  useEffect(() => {
    void reload();
  }, [reload]);

  const save = useCallback(
    async (draft: ScenarioDraft, slug?: string) => {
      setBusy(true);
      setError(null);
      try {
        const saved = await useCases.save.execute(draft, slug);
        await reload();
        return saved;
      } catch (e) {
        setError(toAppError(e));
        return null;
      } finally {
        setBusy(false);
      }
    },
    [useCases.save, reload],
  );

  const remove = useCallback(
    async (slug: string) => {
      setBusy(true);
      setError(null);
      try {
        await useCases.remove.execute(slug);
        await reload();
      } catch (e) {
        setError(toAppError(e));
      } finally {
        setBusy(false);
      }
    },
    [useCases.remove, reload],
  );

  return { scenarios, loading, busy, error, save, remove, reload };
}
