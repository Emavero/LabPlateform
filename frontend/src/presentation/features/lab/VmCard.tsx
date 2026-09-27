import { Link } from 'react-router-dom';
import {
  availableAction,
  isRunning,
  type VirtualMachine,
  type VmAction,
} from '@/domain/models/VirtualMachine';
import { Alert, Button, Icon, StatusIndicator, type Status } from '../../design-system';
import { useI18n } from '../../i18n/I18nContext';
import { ConnectionDetails } from './ConnectionDetails';
import { isSameDay } from './format';

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
  const { t, formatDate, formatTime } = useI18n();
  const action = availableAction(vm);
  const running = isRunning(vm);
  const titleId = `vm-${vm.id}-title`;
  /** L'heure suffit le jour même ; passé minuit, la date est plus parlante. */
  const since = (date: Date) =>
    isSameDay(date, new Date())
      ? t('lab.sinceTime', { time: formatTime(date) })
      : t('lab.sinceDate', { date: formatDate(date) });

  return (
    <article className={['vm-card', running && 'vm-card--running'].filter(Boolean).join(' ')} aria-labelledby={titleId}>
      <header className="vm-card__header">
        <span className={`vm-card__os vm-card__os--${vm.os.toLowerCase()}`}>
          <Icon name={vm.os === 'WINDOWS' ? 'windows' : 'linux'} size={26} />
        </span>
        <div className="vm-card__heading">
          <p className="vm-card__family">{t(`os.${vm.os}`)}</p>
          <h3 className="vm-card__name" id={titleId}>
            {vm.osName}
          </h3>
        </div>
        <StatusIndicator status={statusOf(vm, pending)} />
      </header>

      <dl className="vm-card__meta">
        <div>
          <dt>{t('lab.access')}</dt>
          <dd>{t(vm.os === 'WINDOWS' ? 'lab.accessRdp' : 'lab.accessSsh')}</dd>
        </div>
        <div>
          <dt>{t('lab.startedSince')}</dt>
          <dd>{running && vm.startedAt ? since(vm.startedAt) : '—'}</dd>
        </div>
      </dl>

      {error && <Alert tone="error">{error}</Alert>}

      {running && vm.connection ? (
        <ConnectionDetails connection={vm.connection} />
      ) : (
        <p className="vm-card__idle">
          {t('lab.idle')}
        </p>
      )}

      <footer className="vm-card__footer">
        {!hideDetailLink && (
          <Link className="text-link" to={`/labs/${vm.id}`}>
            {t('lab.consoleAndDetails')} <Icon name="chevronRight" size={14} />
          </Link>
        )}
        <Button
          variant={action === 'start' ? 'success' : 'danger'}
          icon={action === 'start' ? 'play' : 'stop'}
          loading={Boolean(pending)}
          loadingLabel={t(pending === 'start' ? 'lab.starting' : 'lab.stopping')}
          onClick={() => onToggle(vm)}
        >
          {t(action === 'start' ? 'lab.start' : 'lab.stop')}
        </Button>
      </footer>
    </article>
  );
}
