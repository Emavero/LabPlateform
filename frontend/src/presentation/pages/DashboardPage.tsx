import { Link } from 'react-router-dom';
import { displayNameOf } from '@/domain/models/User';
import { isRunning } from '@/domain/models/VirtualMachine';
import { Alert, Button, Icon, Panel, Spinner } from '../design-system';
import { BoxCard } from '../features/box/BoxCard';
import { ProgressPanel } from '../features/box/ProgressPanel';
import { VmCard } from '../features/lab/VmCard';
import { useI18n } from '../i18n/I18nContext';
import { useBoxes } from '../hooks/useBoxes';
import { useLab } from '../hooks/useLab';
import { useProgress } from '../hooks/useProgress';
import { useAuth } from '../state/AuthContext';

/** Salutation du moment. L'heure est celle du poste, comme partout ailleurs. */
function greetingKey(now = new Date()): 'home.morning' | 'home.evening' {
  const hour = now.getHours();
  return hour < 5 || hour >= 18 ? 'home.evening' : 'home.morning';
}

export function DashboardPage() {
  const { t } = useI18n();
  const { user } = useAuth();
  const lab = useLab();
  const { progress } = useProgress();
  const catalogue = useBoxes();
  const running = lab.vms.filter(isRunning).length;
  // Trois cibles mises en avant : le catalogue complet a sa propre page.
  const featured = catalogue.boxes.filter((box) => !box.pwned).slice(0, 3);

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <h1 className="page__title">
            {t('home.greeting', { greeting: t(greetingKey()), name: user ? displayNameOf(user) : '' })}
          </h1>
          <p className="page__lead">
            {lab.loading
              ? t('home.loadingEnvironment')
              : lab.vms.length === 0
                ? t('home.noMachine')
                : running === 0
                  ? t('home.allStopped', { total: lab.vms.length })
                  : t('home.running', { running, total: lab.vms.length })}
          </p>
        </div>
      </header>

      <Panel
        title={t('home.progress')}
        description={t('home.progressHint')}
        actions={
          <Link className="btn btn--ghost btn--sm" to="/scoreboard">
            <Icon name="trophy" size={16} />
            <span>{t('nav.scoreboard')}</span>
          </Link>
        }
      >
        <ProgressPanel progress={progress} />
      </Panel>

      <Panel
        title={t('home.targets')}
        description={t('home.targetsHint')}
        actions={
          <Link className="btn btn--ghost btn--sm" to="/machines">
            <Icon name="target" size={16} />
            <span>{t('home.wholeCatalogue')}</span>
          </Link>
        }
      >
        {catalogue.loading && catalogue.boxes.length === 0 ? (
          <div className="empty">
            <Spinner size={22} label={t('home.loadingMachines')} />
          </div>
        ) : featured.length === 0 ? (
          <p className="empty">
            {t(catalogue.boxes.length === 0 ? 'machines.none' : 'home.allOwned')}
          </p>
        ) : (
          <div className="box-grid">
            {featured.map((box) => (
              <BoxCard key={box.slug} box={box} />
            ))}
          </div>
        )}
      </Panel>

      <Panel
        title={t('home.lab')}
        description={t('home.labHint')}
        actions={
          <Link className="btn btn--ghost btn--sm" to="/labs">
            <Icon name="server" size={16} />
            <span>{t('home.openLab')}</span>
          </Link>
        }
      >
        <LabGrid lab={lab} />
      </Panel>
    </div>
  );
}

/** Grille partagée par le tableau de bord et la page Lab. */
export function LabGrid({ lab }: { lab: ReturnType<typeof useLab> }) {
  const { t } = useI18n();
  if (lab.loading && lab.vms.length === 0) {
    return (
      <div className="empty">
        <Spinner size={22} label={t('home.loadingMachines')} />
      </div>
    );
  }
  if (lab.loadError) {
    return (
      <Alert
        tone="error"
        title={t('home.labLoadError')}
        action={
          <Button variant="ghost" size="sm" icon="refresh" onClick={() => void lab.reload()}>
            {t('common.retry')}
          </Button>
        }
      >
        {lab.loadError.message}
      </Alert>
    );
  }
  if (lab.vms.length === 0) {
    return <p className="empty">{t('home.labEmpty')}</p>;
  }
  return (
    <div className="vm-grid">
      {lab.vms.map((vm) => (
        <VmCard
          key={vm.id}
          vm={vm}
          pending={lab.pending[vm.id]}
          error={lab.actionErrors[vm.id]}
          onToggle={(target) => void lab.toggle(target)}
        />
      ))}
    </div>
  );
}
