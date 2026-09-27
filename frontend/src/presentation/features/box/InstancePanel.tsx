import { isInstanceRunning, minutesLeft, type Box } from '@/domain/models/Box';
import { Button, CopyField, Icon, StatusIndicator } from '../../design-system';
import { useI18n } from '../../i18n/I18nContext';

interface InstancePanelProps {
  box: Box;
  pending: boolean;
  onToggle: () => void;
}

/**
 * Cible lancée à la demande. Tant qu'elle ne tourne pas, la machine n'a pas
 * d'adresse à joindre : c'est le lancement qui en donne une, pour une durée
 * limitée.
 */
export function InstancePanel({ box, pending, onToggle }: InstancePanelProps) {
  const { t } = useI18n();
  const running = isInstanceRunning(box);
  const minutes = minutesLeft(box);

  return (
    <div className="instance">
      <div className="instance__state">
        <StatusIndicator status={pending ? (running ? 'stopping' : 'starting') : running ? 'running' : 'stopped'} />
        {running && (
          <span className="instance__ttl">
            <Icon name="activity" size={14} /> {t('instance.ttl', { minutes })}
          </span>
        )}
      </div>

      {running && box.instanceAddress ? (
        <CopyField label={t('instance.address')} value={box.instanceAddress} />
      ) : (
        <p className="instance__idle">{t('instance.idle')}</p>
      )}

      <Button
        variant={running ? 'danger' : 'success'}
        icon={running ? 'stop' : 'play'}
        loading={pending}
        loadingLabel={t(running ? 'instance.stopping' : 'instance.starting')}
        onClick={onToggle}
      >
        {t(running ? 'instance.stop' : 'instance.start')}
      </Button>
    </div>
  );
}
