import { Link } from 'react-router-dom';
import { BOX_FILTER_LABELS, type BoxFilter } from '@/domain/models/Box';
import { Alert, Button, Icon, Panel, Spinner } from '../design-system';
import { BoxCard } from '../features/box/BoxCard';
import { ProgressPanel } from '../features/box/ProgressPanel';
import { useBoxes } from '../hooks/useBoxes';
import { useProgress } from '../hooks/useProgress';
import { useVpnAccess } from '../hooks/useVpn';

const FILTERS: readonly BoxFilter[] = ['ALL', 'TODO', 'PWNED'];

/** Catalogue des machines à compromettre. */
export function MachinesPage() {
  const catalogue = useBoxes();
  const { progress } = useProgress();
  const vpn = useVpnAccess();

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <h1 className="page__title">Machines</h1>
          <p className="page__lead">
            Compromettez une machine, trouvez ses deux flags et marquez les points de sa difficulté.
          </p>
        </div>
        <Button
          variant="ghost"
          size="sm"
          icon="refresh"
          loading={catalogue.loading}
          onClick={() => void catalogue.reload()}
        >
          Actualiser
        </Button>
      </header>

      <Panel
        title="Votre progression"
        description="Le rang se calcule sur la part du catalogue que vous possédez."
        actions={
          <Link className="btn btn--ghost btn--sm" to="/scoreboard">
            <Icon name="trophy" size={16} />
            <span>Classement</span>
          </Link>
        }
      >
        <ProgressPanel progress={progress} />
      </Panel>

      {vpn.access?.enabled && (
        <Alert
          tone="info"
          title="Les machines vivent derrière le VPN"
          action={
            <Link className="btn btn--ghost btn--sm" to="/vpn">
              VPN Access
            </Link>
          }
        >
          Montez le VPN ({vpn.access.labNetwork}) pour joindre les adresses affichées sur chaque machine.
        </Alert>
      )}

      <div className="tabs" role="tablist" aria-label="Filtrer le catalogue">
        {FILTERS.map((filter) => (
          <button
            key={filter}
            type="button"
            role="tab"
            aria-selected={catalogue.filter === filter}
            className={['tabs__tab', catalogue.filter === filter && 'tabs__tab--active'].filter(Boolean).join(' ')}
            onClick={() => catalogue.setFilter(filter)}
          >
            {BOX_FILTER_LABELS[filter]}
          </button>
        ))}
      </div>

      <BoxGrid catalogue={catalogue} />
    </div>
  );
}

function BoxGrid({ catalogue }: { catalogue: ReturnType<typeof useBoxes> }) {
  if (catalogue.loading && catalogue.boxes.length === 0) {
    return (
      <div className="empty">
        <Spinner size={22} label="Chargement du catalogue" />
      </div>
    );
  }
  if (catalogue.error) {
    return (
      <Alert
        tone="error"
        title="Impossible de charger le catalogue"
        action={
          <Button variant="ghost" size="sm" icon="refresh" onClick={() => void catalogue.reload()}>
            Réessayer
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
        {catalogue.boxes.length === 0
          ? "Aucune machine n'est publiée pour le moment."
          : 'Aucune machine ne correspond à ce filtre.'}
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
