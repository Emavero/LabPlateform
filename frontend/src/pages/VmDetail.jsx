import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { AppLayout } from '../components/AppLayout';
import { labApi } from '../api/labApi';

const OS_LABELS = {
  WINDOWS: 'Windows',
  LINUX: 'Linux',
};

export function VmDetail() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [vm, setVm] = useState(null);
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      const [vmData, logsData] = await Promise.all([labApi.getVm(id), labApi.getLogs(id)]);
      setVm(vmData);
      setLogs(logsData);
    } catch (err) {
      setError('Impossible de charger cette machine.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    load();
  }, [load]);

  const handleToggle = async () => {
    setBusy(true);
    setError('');
    try {
      const updated = vm.status === 'RUNNING' ? await labApi.stopVm(id) : await labApi.startVm(id);
      setVm(updated);
      const logsData = await labApi.getLogs(id);
      setLogs(logsData);
    } catch (err) {
      setError("Échec de l'opération sur la machine.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <AppLayout>
      <button className="back-link" onClick={() => navigate('/labs')}>
        Retour à mes Labs
      </button>

      {loading ? (
        <div className="loading-text">Chargement…</div>
      ) : !vm ? (
        <div className="alert-error">{error || 'Machine introuvable.'}</div>
      ) : (
        <>
          <div className="page__header vm-detail__header">
            <div>
              <div className="eyebrow">Console</div>
              <h1>Machine {OS_LABELS[vm.type]}</h1>
              <p>Console et informations de connexion</p>
            </div>
            <span className={`badge ${vm.status === 'RUNNING' ? 'running' : 'stopped'}`}>
              {vm.status === 'RUNNING' ? 'En cours' : 'Arrêtée'}
            </span>
          </div>

          {error && <div className="alert-error">{error}</div>}

          <div className="vm-detail-layout">
            <section className="card vm-detail-card">
              <h2>Connexion</h2>
              {vm.status === 'RUNNING' ? (
                <div className="vm-card__info">
                  <div className="vm-card__info-row">
                    <span>Adresse IP</span>
                    <span>{vm.ipAddress}</span>
                  </div>
                  <div className="vm-card__info-row">
                    <span>Port</span>
                    <span>{vm.port}</span>
                  </div>
                  <div className="vm-card__info-row">
                    <span>Utilisateur</span>
                    <span>{vm.username}</span>
                  </div>
                  <div className="vm-card__info-row">
                    <span>Mot de passe</span>
                    <span>{vm.accessPassword}</span>
                  </div>
                </div>
              ) : (
                <div className="vm-card__info">
                  <div className="vm-card__info-empty">
                    Démarrez la machine pour afficher les informations de connexion
                  </div>
                </div>
              )}

              <button
                className={`btn-action ${vm.status === 'RUNNING' ? 'stop' : 'start'}`}
                disabled={busy}
                onClick={handleToggle}
              >
                {busy ? 'Traitement…' : vm.status === 'RUNNING' ? 'Arrêter la machine' : 'Démarrer la machine'}
              </button>
            </section>

            <section className="card vm-detail-card">
              <h2>Console</h2>
              <div className="console-box mono">
                {logs.map((line, i) => (
                  <div key={i} className="console-box__line">
                    {line}
                  </div>
                ))}
              </div>
            </section>
          </div>
        </>
      )}
    </AppLayout>
  );
}
