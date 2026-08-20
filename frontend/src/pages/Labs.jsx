import { useCallback, useEffect, useState } from 'react';
import { AppLayout } from '../components/AppLayout';
import { VmCard } from '../components/VmCard';
import { labApi } from '../api/labApi';

export function Labs() {
  const [vms, setVms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);

  const loadVms = useCallback(async () => {
    try {
      const data = await labApi.listVms();
      setVms(data);
    } catch (err) {
      setError("Impossible de charger vos machines. Réessayez.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadVms();
  }, [loadVms]);

  const handleStart = async (id) => {
    setBusyId(id);
    try {
      const updated = await labApi.startVm(id);
      setVms((prev) => prev.map((vm) => (vm.id === id ? updated : vm)));
    } catch (err) {
      setError('Échec du démarrage de la machine.');
    } finally {
      setBusyId(null);
    }
  };

  const handleStop = async (id) => {
    setBusyId(id);
    try {
      const updated = await labApi.stopVm(id);
      setVms((prev) => prev.map((vm) => (vm.id === id ? updated : vm)));
    } catch (err) {
      setError("Échec de l'arrêt de la machine.");
    } finally {
      setBusyId(null);
    }
  };

  return (
    <AppLayout>
      <div className="page__header">
        <h1>Mes Labs</h1>
        <p>Démarrez une machine pour afficher ses informations de connexion</p>
      </div>

      {error && <div className="alert-error">{error}</div>}

      {loading ? (
        <div className="loading-text">Chargement des machines…</div>
      ) : (
        <div className="vm-grid">
          {vms.map((vm) => (
            <VmCard key={vm.id} vm={vm} onStart={handleStart} onStop={handleStop} busy={busyId === vm.id} />
          ))}
        </div>
      )}
    </AppLayout>
  );
}
