import { Link } from 'react-router-dom';
import { availableAction, reachableAddress } from '@/domain/models/TargetMachine';
import { Alert, Button, CopyField, Icon, Panel, Spinner } from '../../design-system';

import { useI18n } from '../../i18n/I18nContext';
import { useTargetMachine } from '../../hooks/useTargetMachine';

/**
 * Bouton « Démarrer / Arrêter » de la cible partagée.
 * <p>
 * Un seul bouton, dont le libellé dit ce qu'il va faire et non l'état courant :
 * l'état, c'est le badge qui le porte. Pendant une transition, le bouton est
 * inactif et l'état se redemande tout seul — rien à cliquer, rien à rafraîchir.
 * <p>
 * L'adresse interne n'apparaît qu'une fois la machine en marche, et toujours
 * accompagnée du rappel du VPN : sans tunnel monté, cette adresse ne mène nulle
 * part, et l'afficher seule ferait conclure à une machine en panne.
 */
export function MachineControl({ id }: { id?: string } = {}) {
  const { t } = useI18n();
  const target = useTargetMachine();
  const machine = target.machine;

  if (target.loading && !machine) {
    return (
      <Panel id={id} title={t('machine.title')}>
        <div className="empty">
          <Spinner size={20} label={t('machine.loading')} />
        </div>
      </Panel>
    );
  }

  if (!machine) {
    return (
      <Panel id={id} title={t('machine.title')}>
        <Alert
          tone="error"
          title={t('machine.loadError')}
          action={
            <Button variant="ghost" size="sm" icon="refresh" onClick={() => void target.reload()}>
              {t('common.retry')}
            </Button>
          }
        >
          {target.error?.message}
        </Alert>
      </Panel>
    );
  }

  const action = availableAction(machine);
  const address = reachableAddress(machine);
  const busy = machine.transitioning;

  return (
    <Panel
      id={id}
      className="machine-control"
      title={t('machine.title')}
      description={t('machine.lead')}
      actions={
        <span
          className={['machine-badge', `machine-badge--${machine.status.toLowerCase()}`].join(' ')}
          // Le badge change sans que l'utilisateur agisse : il est annoncé.
          role="status"
        >
          {busy && <Spinner size={13} />}
          {machine.statusName}
        </span>
      }
    >
      <div className="machine-control__row">
        <Button
          variant={action === 'start' ? 'success' : 'ghost'}
          icon={action === 'start' ? 'play' : 'stop'}
          disabled={busy}
          loading={target.pending !== null}
          loadingLabel={t(target.pending === 'stop' ? 'machine.stopping' : 'machine.starting')}
          onClick={() => void target.toggle()}
        >
          {t(action === 'start' ? 'machine.start' : 'machine.stop')}
        </Button>

        {busy && <p className="machine-control__hint">{t('machine.watching')}</p>}
      </div>

      {target.gaveUp && (
        <Alert
          tone="info"
          title={t('machine.gaveUpTitle')}
          action={
            <Button variant="ghost" size="sm" icon="refresh" onClick={() => void target.reload()}>
              {t('common.refresh')}
            </Button>
          }
        >
          {t('machine.gaveUpText')}
        </Alert>
      )}

      {target.error && <Alert tone="error">{target.error.message}</Alert>}

      {address && (
        <div className="machine-control__access">
          <CopyField label={t('machine.internalIp')} value={address} />
          <p className="machine-control__hint">
            <Icon name="vpn" size={14} />{' '}
            {t('machine.vpnReminder')} <Link className="text-link" to="/vpn">{t('machine.vpnLink')}</Link>
          </p>
        </div>
      )}
    </Panel>
  );
}
