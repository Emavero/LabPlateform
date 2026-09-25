import { Link } from 'react-router-dom';
import {
  availableAction,
  isRunning,
  OS_FAMILY_LABELS,
  type VirtualMachine,
  type VmAction,
} from '@/domain/models/VirtualMachine';
import { Alert, Button, Icon, StatusIndicator, type Status } from '../../design-system';
import { ConnectionDetails } from './ConnectionDetails';
import { formatUptimeSince } from './format';

interface VmCardProps {
  vm: VirtualMachine;
  pending?: VmAction;
  error?: string;
  onToggle: (vm: VirtualMachine) => void;
  /** Masque le lien vers la fiche détaillée (quand on y est déjà). */
  hideDetailLink?: boolean;
}

export function statusOf(vm: VirtualMachine, pending?: VmAction): Status {
  if (pending === 'start') return 'starting';
  if (pending === 'stop') return 'stopping';
  return isRunning(vm) ? 'running' : 'stopped';
}

export function VmCard({ vm, pending, error, onToggle, hideDetailLink = false }: VmCardProps) {
  const action = availableAction(vm);
  const running = isRunning(vm);
  const titleId = `vm-${vm.id}-title`;

  return (
    <article className={['vm-card', running && 'vm-card--running'].filter(Boolean).join(' ')} aria-labelledby={titleId}>
      <header className="vm-card__header">
        <span className={`vm-card__os vm-card__os--${vm.os.toLowerCase()}`}>
          <Icon name={vm.os === 'WINDOWS' ? 'windows' : 'linux'} size={26} />
        </span>
        <div className="vm-card__heading">
          <p className="vm-card__family">{OS_FAMILY_LABELS[vm.os]}</p>
          <h3 className="vm-card__name" id={titleId}>
            {vm.osName}
          </h3>
        </div>
        <StatusIndicator status={statusOf(vm, pending)} />
      </header>

      <dl className="vm-card__meta">
        <div>
          <dt>Accès</dt>
          <dd>{vm.os === 'WINDOWS' ? 'RDP, port 3389' : 'SSH, port 22'}</dd>
        </div>
        <div>
          <dt>Démarrée</dt>
          <dd>{running && vm.startedAt ? formatUptimeSince(vm.startedAt) : '—'}</dd>
        </div>
      </dl>

      {error && <Alert tone="error">{error}</Alert>}

      {running && vm.connection ? (
        <ConnectionDetails connection={vm.connection} />
      ) : (
        <p className="vm-card__idle">
          La machine est arrêtée. Démarrez-la pour obtenir son adresse et des identifiants temporaires.
        </p>
      )}

      <footer className="vm-card__footer">
        {!hideDetailLink && (
          <Link className="text-link" to={`/labs/${vm.id}`}>
            Console & détails <Icon name="chevronRight" size={14} />
          </Link>
        )}
        <Button
          variant={action === 'start' ? 'success' : 'danger'}
          icon={action === 'start' ? 'play' : 'stop'}
          loading={Boolean(pending)}
          loadingLabel={pending === 'start' ? 'Démarrage…' : 'Arrêt…'}
          onClick={() => onToggle(vm)}
        >
          {action === 'start' ? 'Démarrer' : 'Arrêter'}
        </Button>
      </footer>
    </article>
  );
}
