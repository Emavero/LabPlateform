import { Link, useParams } from 'react-router-dom';
import { Alert, Button, Icon, Panel, Spinner } from '../design-system';
import { VmCard } from '../features/lab/VmCard';
import { useI18n } from '../i18n/I18nContext';
import { useVmDetail } from '../hooks/useVmDetail';
import { NotFoundPage } from './NotFoundPage';

export function VmDetailPage() {
  const { id } = useParams();
  const vmId = Number(id);
  if (!Number.isInteger(vmId) || vmId <= 0) return <NotFoundPage />;
  return <VmDetail id={vmId} />;
}

function VmDetail({ id }: { id: number }) {
  const { t } = useI18n();
  const { vm, log, loading, error, pending, toggle, reload } = useVmDetail(id);

  if (loading && !vm) {
    return (
      <div className="page empty">
        <Spinner size={22} label={t('lab.loadingMachine')} />
      </div>
    );
  }
  if (!vm) {
    if (error?.kind === 'not_found') return <NotFoundPage />;
    return (
      <div className="page">
        <Alert
          tone="error"
          title={t('lab.unavailable')}
          action={
            <Button variant="ghost" size="sm" icon="refresh" onClick={() => void reload()}>
              {t('common.retry')}
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
      <Link className="text-link back-link" to="/machines">
        <Icon name="arrowLeft" size={16} /> {t('nav.machines')}
      </Link>
      <header className="page__header">
        <div>
          <p className="page__eyebrow">{t('lab.machineNumber', { id: vm.id })}</p>
          <h1 className="page__title">{vm.osName}</h1>
        </div>
      </header>
      <div className="detail-grid">
        <VmCard vm={vm} pending={pending ?? undefined} error={error?.message} onToggle={() => void toggle()} hideDetailLink />
        <Panel
          title={t('lab.console')}
          description={t('lab.consoleHint')}
          actions={
            <Button variant="ghost" size="sm" icon="refresh" loading={loading} onClick={() => void reload()}>
              {t('common.refresh')}
            </Button>
          }
        >
          <pre className="console" tabIndex={0} aria-label={t('lab.consoleLabel')}>
            {log.length > 0 ? log.join('\n') : t('lab.consoleEmpty')}
          </pre>
        </Panel>
      </div>
    </div>
  );
}
