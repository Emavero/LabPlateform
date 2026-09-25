export type Status = 'running' | 'stopped' | 'starting' | 'stopping';

const LABELS: Record<Status, string> = {
  running: "En cours d'exécution",
  stopped: 'Arrêtée',
  starting: 'Démarrage…',
  stopping: 'Arrêt…',
};

export function StatusIndicator({ status }: { status: Status }) {
  return (
    <span className={`status status--${status}`}>
      <span className="status__dot" aria-hidden="true" />
      {LABELS[status]}
    </span>
  );
}
