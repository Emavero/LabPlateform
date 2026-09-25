import { useCallback, useEffect, useRef, useState } from 'react';
import { toAppError, type AppError } from '@/domain/errors/AppError';

interface ActionState<TArgs extends unknown[], TResult> {
  /** Exécute l'action ; renvoie undefined en cas d'échec (l'erreur est exposée dans `error`). */
  run: (...args: TArgs) => Promise<TResult | undefined>;
  pending: boolean;
  error: AppError | null;
  fieldErrors: Readonly<Record<string, string>>;
  reset: () => void;
}

/** Gère l'état d'une action asynchrone de formulaire : chargement, erreur globale, erreurs par champ. */
export function useAction<TArgs extends unknown[], TResult>(
  action: (...args: TArgs) => Promise<TResult>,
): ActionState<TArgs, TResult> {
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<AppError | null>(null);
  const mounted = useRef(true);

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  const run = useCallback(
    async (...args: TArgs) => {
      setPending(true);
      setError(null);
      try {
        return await action(...args);
      } catch (e) {
        if (mounted.current) setError(toAppError(e));
        return undefined;
      } finally {
        if (mounted.current) setPending(false);
      }
    },
    [action],
  );

  const reset = useCallback(() => setError(null), []);

  return { run, pending, error, fieldErrors: error?.fieldErrors ?? {}, reset };
}
