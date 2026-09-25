import { useCallback, useEffect, useState } from 'react';
import { toAppError, type AppError } from '@/domain/errors/AppError';
import { availableAction, type VirtualMachine, type VmAction } from '@/domain/models/VirtualMachine';
import { useDependencies } from '../state/DependenciesContext';

export interface LabState {
  vms: VirtualMachine[];
  loading: boolean;
  loadError: AppError | null;
  /** Action en cours par machine : plusieurs machines peuvent changer d'état en parallèle. */
  pending: Readonly<Record<number, VmAction>>;
  actionErrors: Readonly<Record<number, string>>;
  toggle: (vm: VirtualMachine) => Promise<void>;
  reload: () => Promise<void>;
}

export function useLab(): LabState {
  const { lab } = useDependencies();
  const [vms, setVms] = useState<VirtualMachine[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<AppError | null>(null);
  const [pending, setPending] = useState<Record<number, VmAction>>({});
  const [actionErrors, setActionErrors] = useState<Record<number, string>>({});

  const reload = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      setVms(await lab.list.execute());
    } catch (e) {
      setLoadError(toAppError(e));
    } finally {
      setLoading(false);
    }
  }, [lab.list]);

  useEffect(() => {
    void reload();
  }, [reload]);

  const toggle = useCallback(
    async (vm: VirtualMachine) => {
      const action = availableAction(vm);
      setPending((p) => ({ ...p, [vm.id]: action }));
      setActionErrors(({ [vm.id]: _removed, ...rest }) => rest);
      try {
        const updated = await lab.runAction.execute(vm.id, action);
        setVms((list) => list.map((item) => (item.id === updated.id ? updated : item)));
      } catch (e) {
        const error = toAppError(e);
        setActionErrors((errors) => ({ ...errors, [vm.id]: error.message }));
        // Un conflit signifie que l'état affiché est périmé : on le resynchronise.
        if (error.kind === 'conflict') {
          lab.get.execute(vm.id).then(
            (fresh) => setVms((list) => list.map((item) => (item.id === fresh.id ? fresh : item))),
            () => undefined,
          );
        }
      } finally {
        setPending(({ [vm.id]: _done, ...rest }) => rest);
      }
    },
    [lab.runAction, lab.get],
  );

  return { vms, loading, loadError, pending, actionErrors, toggle, reload };
}
