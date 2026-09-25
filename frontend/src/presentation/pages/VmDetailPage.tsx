import { Link, useParams } from 'react-router-dom';
import { Alert, Button, Icon, Panel, Spinner } from '../design-system';
import { VmCard } from '../features/lab/VmCard';
import { useVmDetail } from '../hooks/useVmDetail';
import { NotFoundPage } from './NotFoundPage';

export function VmDetailPage() {
  const { id } = useParams();
  const vmId = Number(id);
  if (!Number.isInteger(vmId) || vmId <= 0) return <NotFoundPage />;
  return <VmDetail id={vmId} />;
}

function VmDetail({ id }: { id: number }) {
  const { vm, log, loading, error, pending, toggle, reload } = useVmDetail(id);

  if (loading && !vm) {
    return (
      <div className="page empty">
        <Spinner size={22} label="Chargement de la machine" />
      </div>
    );
  }
  if (!vm) {
    if (error?.kind === 'not_found') return <NotFoundPage />;
    return (
      <div className="page">
        <Alert
          tone="error"
          title="Machine indisponible"
          action={
            <Button variant="ghost" size="sm" icon="refresh" onClick={() => void reload()}>
              Réessayer
            </Button>
          }
        >
          {error?.message}
        </Alert>
      </div>
    );
  }

  return (
    <div className="page">
      <Link className="text-link back-link" to="/labs">
        <Icon name="arrowLeft" size={16} /> Lab Infrastructure
      </Link>
      <header className="page__header">
        <div>
          <p className="page__eyebrow">Machine #{vm.id}</p>
          <h1 className="page__title">{vm.osName}</h1>
        </div>
      </header>
      <div className="detail-grid">
        <VmCard vm={vm} pending={pending ?? undefined} error={error?.message} onToggle={() => void toggle()} hideDetailLink />
        <Panel
          title="Console"
          description="Journal de démarrage de la machine."
          actions={
            <Button variant="ghost" size="sm" icon="refresh" loading={loading} onClick={() => void reload()}>
              Actualiser
            </Button>
          }
        >
          <pre className="console" tabIndex={0} aria-label="Journal de la console">
            {log.length > 0 ? log.join('\n') : 'Aucune sortie pour le moment.'}
          </pre>
        </Panel>
      </div>
    </div>
  );
}
