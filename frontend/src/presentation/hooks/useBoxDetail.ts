import { useCallback, useEffect, useState } from 'react';
import type { Box, FlagKind } from '@/domain/models/Box';
import { toAppError, type AppError } from '@/domain/errors/AppError';
import type { FlagSubmission } from '@/domain/repositories/BoxRepository';
import { useDependencies } from '../state/DependenciesContext';

export interface BoxDetailState {
  box: Box | null;
  loading: boolean;
  error: AppError | null;
  /** Dernière soumission acceptée : sert à féliciter le joueur. */
  lastSubmission: FlagSubmission | null;
  submitting: FlagKind | null;
  submitError: AppError | null;
  submit: (kind: FlagKind, flag: string) => Promise<boolean>;
  reload: () => Promise<void>;
}

export function useBoxDetail(slug: string): BoxDetailState {
  const { boxes } = useDependencies();
  const [box, setBox] = useState<Box | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<AppError | null>(null);
  const [lastSubmission, setLastSubmission] = useState<FlagSubmission | null>(null);
  const [submitting, setSubmitting] = useState<FlagKind | null>(null);
  const [submitError, setSubmitError] = useState<AppError | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setBox(await boxes.get.execute(slug));
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setLoading(false);
    }
  }, [boxes.get, slug]);

  useEffect(() => {
    void reload();
  }, [reload]);

  const submit = useCallback(
    async (kind: FlagKind, flag: string) => {
      setSubmitting(kind);
      setSubmitError(null);
      try {
        setLastSubmission(await boxes.submitFlag.execute(slug, kind, flag));
        // La fiche porte l'état de possession : elle est relue après un succès.
        setBox(await boxes.get.execute(slug));
        return true;
      } catch (e) {
        setSubmitError(toAppError(e));
        return false;
      } finally {
        setSubmitting(null);
      }
    },
    [boxes.submitFlag, boxes.get, slug],
  );

  return { box, loading, error, lastSubmission, submitting, submitError, submit, reload };
}
