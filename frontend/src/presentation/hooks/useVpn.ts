import { useCallback, useEffect, useState } from 'react';
import { toAppError, type AppError } from '@/domain/errors/AppError';
import type { VpnAccess } from '@/domain/models/Vpn';
import { useDependencies } from '../state/DependenciesContext';

/** État de l'accès VPN de l'utilisateur connecté. */
export function useVpnAccess() {
  const { vpn } = useDependencies();
  const [access, setAccess] = useState<VpnAccess | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<AppError | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    try {
      setAccess(await vpn.getAccess.execute());
      setLoadError(null);
    } catch (e) {
      setLoadError(toAppError(e));
    } finally {
      setLoading(false);
    }
  }, [vpn.getAccess]);

  useEffect(() => {
    void reload();
  }, [reload]);

  return { access, setAccess, loading, loadError, reload };
}
