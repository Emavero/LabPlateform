import { isInstanceRunning, minutesLeft, type Box } from '@/domain/models/Box';
import { Button, CopyField, Icon, StatusIndicator } from '../../design-system';

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
  const running = isInstanceRunning(box);
  const minutes = minutesLeft(box);

  return (
    <div className="instance">
      <div className="instance__state">
        <StatusIndicator status={pending ? (running ? 'stopping' : 'starting') : running ? 'running' : 'stopped'} />
        {running && (
          <span className="instance__ttl">
            <Icon name="activity" size={14} /> {minutes} min avant extinction
          </span>
        )}
      </div>

      {running && box.instanceAddress ? (
        <CopyField label="Adresse de la cible" value={box.instanceAddress} />
      ) : (
        <p className="instance__idle">
          Lancez la machine pour obtenir son adresse. Une seule cible tourne à la fois, et elle s'éteint
          automatiquement passé son délai.
        </p>
      )}

      <Button
        variant={running ? 'danger' : 'success'}
        icon={running ? 'stop' : 'play'}
        loading={pending}
        loadingLabel={running ? 'Arrêt…' : 'Lancement…'}
        onClick={onToggle}
      >
        {running ? 'Arrêter la machine' : 'Lancer la machine'}
      </Button>
    </div>
  );
}
