import { useCallback, useEffect, useState } from 'react';
import { toAppError, type AppError } from '@/domain/errors/AppError';
import { availableAction, type VirtualMachine, type VmAction } from '@/domain/models/VirtualMachine';
import { useDependencies } from '../state/DependenciesContext';

export function useVmDetail(id: number) {
  const { lab } = useDependencies();
  const [vm, setVm] = useState<VirtualMachine | null>(null);
  const [log, setLog] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<AppError | null>(null);
  const [pending, setPending] = useState<VmAction | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [machine, lines] = await Promise.all([lab.get.execute(id), lab.console.execute(id)]);
      setVm(machine);
      setLog(lines);
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setLoading(false);
    }
  }, [id, lab.get, lab.console]);

  useEffect(() => {
    void load();
  }, [load]);

  const toggle = useCallback(async () => {
    if (!vm) return;
    const action = availableAction(vm);
    setPending(action);
    setError(null);
    try {
      setVm(await lab.runAction.execute(vm.id, action));
      setLog(await lab.console.execute(vm.id));
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setPending(null);
    }
  }, [vm, lab.runAction, lab.console]);

  return { vm, log, loading, error, pending, toggle, reload: load };
}
