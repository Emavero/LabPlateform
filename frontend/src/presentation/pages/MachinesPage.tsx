import { Link } from 'react-router-dom';
import type { BoxFilter } from '@/domain/models/Box';
import { needsProfileBeforeStarting } from '@/domain/models/Vpn';
import { Alert, Button, Icon, Panel, Spinner } from '../design-system';
import { BoxCard } from '../features/box/BoxCard';
import { ProgressPanel } from '../features/box/ProgressPanel';
import { MachineControl } from '../features/lab/MachineControl';
import { useI18n } from '../i18n/I18nContext';
import { useBoxes } from '../hooks/useBoxes';
import { useLab } from '../hooks/useLab';
import { useProgress } from '../hooks/useProgress';
import { useVpnAccess } from '../hooks/useVpn';
import { LabGrid } from './DashboardPage';

const FILTERS: readonly BoxFilter[] = ['ALL', 'TODO', 'PWNED'];

/**
 * Tout ce qui tourne : les cibles à compromettre, et les machines depuis
 * lesquelles on les attaque.
 * <p>
 * Les deux vivaient sur deux pages — « Machines » et « Infrastructure du lab » —
 * qui répétaient le même rappel de VPN et obligeaient à aller-retour pour
 * démarrer un poste de travail avant de choisir une cible. C'est une seule
 * activité, elle tient sur une seule page.
 */
export function MachinesPage() {
  const { t } = useI18n();
  const catalogue = useBoxes();
  const lab = useLab();
  const { progress } = useProgress();
  const vpn = useVpnAccess();
  const profileMissing = needsProfileBeforeStarting(vpn.access);

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
          tone={profileMissing ? 'warning' : 'info'}
          title={t(profileMissing ? 'machines.vpnMissingTitle' : 'machines.vpnTitle')}
          action={
            <Link className="btn btn--ghost btn--sm" to="/vpn">
              {t(profileMissing ? 'machines.vpnGet' : 'nav.vpn')}
            </Link>
          }
        >
          {t(profileMissing ? 'machines.vpnMissingText' : 'machines.vpnText', {
            network: vpn.access.labNetwork,
          })}
        </Alert>
      )}

      {/* La cible partagée, puis les machines de travail : on allume avant
          d'attaquer, et c'est l'adresse de la première qu'on vise. */}
      <MachineControl />

      <Panel title={t('machines.workstations')} description={t('machines.workstationsHint')}>
        <LabGrid lab={lab} />
      </Panel>

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
