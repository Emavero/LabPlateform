import { useCallback, useEffect, useState } from 'react';
import type { Billing, BillingPeriod, PaymentMethod } from '@/domain/models/Billing';
import { toAppError, type AppError } from '@/domain/errors/AppError';
import { useLanguageRefresh } from './useLanguageRefresh';
import { useDependencies } from '../state/DependenciesContext';

export interface BillingState {
  billing: Billing | null;
  loading: boolean;
  /** Vrai pendant l'ouverture du paiement ou la résiliation. */
  busy: boolean;
  error: AppError | null;
  subscribe: (method: PaymentMethod, period: BillingPeriod) => Promise<void>;
  confirm: (reference: string) => Promise<boolean>;
  cancel: () => Promise<void>;
  reload: () => Promise<void>;
}

/**
 * Abonnement du compte connecté.
 * <p>
 * L'ouverture du paiement se termine par une sortie de l'application : le
 * prestataire héberge la page de saisie, et nous n'en voulons pas dans une
 * iframe — un champ de carte doit rester sur le domaine de celui qui l'encaisse.
 */
export function useBilling(): BillingState {
  const { billing: useCases } = useDependencies();
  const [billing, setBilling] = useState<Billing | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<AppError | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setBilling(await useCases.get.execute());
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setLoading(false);
    }
  }, [useCases.get]);

  useEffect(() => {
    void reload();
  }, [reload]);
  useLanguageRefresh(reload);

  const subscribe = useCallback(
    async (method: PaymentMethod, period: BillingPeriod) => {
      setBusy(true);
      setError(null);
      try {
        const checkout = await useCases.startCheckout.execute(method, period);
        // Remplacement de l'adresse plutôt qu'un nouvel onglet : un bloqueur de
        // fenêtres ne doit pas pouvoir interrompre un paiement en cours.
        window.location.assign(checkout.redirectUrl);
      } catch (e) {
        setError(toAppError(e));
        setBusy(false);
      }
    },
    [useCases.startCheckout],
  );

  const confirm = useCallback(
    async (reference: string) => {
      setBusy(true);
      setError(null);
      try {
        setBilling(await useCases.confirm.execute(reference));
        return true;
      } catch (e) {
        setError(toAppError(e));
        return false;
      } finally {
        setBusy(false);
        setLoading(false);
      }
    },
    [useCases.confirm],
  );

  const cancel = useCallback(async () => {
    setBusy(true);
    setError(null);
    try {
      setBilling(await useCases.cancel.execute());
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setBusy(false);
    }
  }, [useCases.cancel]);

  return { billing, loading, busy, error, subscribe, confirm, cancel, reload };
}
