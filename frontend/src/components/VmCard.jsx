import { Link } from 'react-router-dom';

const OS_LABELS = {
  WINDOWS: { label: 'Windows Server', icon: 'WIN', className: 'windows' },
  LINUX: { label: 'Ubuntu Linux', icon: 'LNX', className: 'linux' },
};

export function VmCard({ vm, onStart, onStop, busy }) {
  const os = OS_LABELS[vm.type];
  const isRunning = vm.status === 'RUNNING';

  return (
    <div className="vm-card">
      <div className="vm-card__head">
        <div className="vm-card__os">
          <div className={`vm-card__os-icon ${os.className}`}>{os.icon}</div>
          {os.label}
        </div>
        <span className={`badge ${isRunning ? 'running' : 'stopped'}`}>
          {isRunning ? 'En cours' : 'Arrêtée'}
        </span>
      </div>

      <div className="vm-card__info">
        {isRunning ? (
          <>
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
          </>
        ) : (
          <div className="vm-card__info-empty">
            Démarrez la machine pour afficher les informations de connexion.
          </div>
        )}
      </div>

      {isRunning ? (
        <button className="btn-action stop" disabled={busy} onClick={() => onStop(vm.id)}>
          {busy ? 'Arrêt en cours…' : 'Arrêter'}
        </button>
      ) : (
        <button className="btn-action start" disabled={busy} onClick={() => onStart(vm.id)}>
          {busy ? 'Démarrage…' : 'Démarrer'}
        </button>
      )}

      <Link className="vm-card__details-link" to={`/labs/${vm.id}`}>
        Voir la console
      </Link>
    </div>
  );
}
