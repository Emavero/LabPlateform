import { Link } from 'react-router-dom';
import type { BoxFilter } from '@/domain/models/Box';
import { Alert, Button, Icon, Panel, Spinner } from '../design-system';
import { BoxCard } from '../features/box/BoxCard';
import { ProgressPanel } from '../features/box/ProgressPanel';
import { useI18n } from '../i18n/I18nContext';
import { useBoxes } from '../hooks/useBoxes';
import { useProgress } from '../hooks/useProgress';
import { useVpnAccess } from '../hooks/useVpn';

const FILTERS: readonly BoxFilter[] = ['ALL', 'TODO', 'PWNED'];

/** Catalogue des machines à compromettre. */
export function MachinesPage() {
  const { t } = useI18n();
  const catalogue = useBoxes();
  const { progress } = useProgress();
  const vpn = useVpnAccess();

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <h1 className="page__title">{t('machines.title')}</h1>
          <p className="page__lead">{t('machines.lead')}</p>
        </div>
        <Button
          variant="ghost"
          size="sm"
          icon="refresh"
          loading={catalogue.loading}
          onClick={() => void catalogue.reload()}
        >
          {t('common.refresh')}
        </Button>
      </header>

      <Panel
        title={t('machines.progress')}
        description={t('machines.progressHint')}
        actions={
          <Link className="btn btn--ghost btn--sm" to="/scoreboard">
            <Icon name="trophy" size={16} />
            <span>{t('nav.scoreboard')}</span>
          </Link>
        }
      >
        <ProgressPanel progress={progress} />
      </Panel>

      {vpn.access?.enabled && (
        <Alert
          tone="info"
          title={t('machines.vpnTitle')}
          action={
            <Link className="btn btn--ghost btn--sm" to="/vpn">
              {t('nav.vpn')}
            </Link>
          }
        >
          {t('machines.vpnText', { network: vpn.access.labNetwork })}
        </Alert>
      )}

      <div className="tabs" role="tablist" aria-label={t('machines.filterGroup')}>
        {FILTERS.map((filter) => (
          <button
            key={filter}
            type="button"
            role="tab"
            aria-selected={catalogue.filter === filter}
            className={['tabs__tab', catalogue.filter === filter && 'tabs__tab--active'].filter(Boolean).join(' ')}
            onClick={() => catalogue.setFilter(filter)}
          >
            {t(`machines.filter.${filter}`)}
          </button>
        ))}
      </div>

      <BoxGrid catalogue={catalogue} />
    </div>
  );
}

function BoxGrid({ catalogue }: { catalogue: ReturnType<typeof useBoxes> }) {
  const { t } = useI18n();
  if (catalogue.loading && catalogue.boxes.length === 0) {
    return (
      <div className="empty">
        <Spinner size={22} label={t('machines.loading')} />
      </div>
    );
  }
  if (catalogue.error) {
    return (
      <Alert
        tone="error"
        title={t('machines.loadError')}
        action={
          <Button variant="ghost" size="sm" icon="refresh" onClick={() => void catalogue.reload()}>
            {t('common.retry')}
          </Button>
        }
      >
        {catalogue.error.message}
      </Alert>
    );
  }
  if (catalogue.visible.length === 0) {
    return (
      <p className="empty">
        {t(catalogue.boxes.length === 0 ? 'machines.none' : 'machines.noneForFilter')}
      </p>
    );
  }
  return (
    <div className="box-grid">
      {catalogue.visible.map((box) => (
        <BoxCard key={box.slug} box={box} />
      ))}
    </div>
  );
}
