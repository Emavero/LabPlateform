import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import type { AdminOverview } from '@/domain/models/Admin';
import { toAppError, type AppError } from '@/domain/errors/AppError';
import { Alert, Button, Icon, Panel, Spinner } from '../design-system';
import { useDependencies } from '../state/DependenciesContext';

/** Vue d'ensemble de l'administrateur : ce qui est publié, ce qui est utilisé. */
export function AdminDashboardPage() {
  const { admin } = useDependencies();
  const [overview, setOverview] = useState<AdminOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<AppError | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setOverview(await admin.overview.execute());
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setLoading(false);
    }
  }, [admin.overview]);

  useEffect(() => {
    void reload();
  }, [reload]);

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="page__eyebrow">Administration</p>
          <h1 className="page__title">Tableau de bord</h1>
          <p className="page__lead">
            Ce que les utilisateurs voient est publié depuis ici : les cours, leurs sections et leurs vidéos.
          </p>
        </div>
        <Button variant="ghost" size="sm" icon="refresh" loading={loading} onClick={() => void reload()}>
          Actualiser
        </Button>
      </header>

      {error && (
        <Alert
          tone="error"
          title="Impossible de charger les chiffres"
          action={
            <Button variant="ghost" size="sm" icon="refresh" onClick={() => void reload()}>
              Réessayer
            </Button>
          }
        >
          {error.message}
        </Alert>
      )}

      <Panel
        title="Contenu publié"
        actions={
          <Link className="btn btn--primary btn--sm" to="/admin/cours">
            <Icon name="book" size={16} />
            <span>Gérer les cours</span>
          </Link>
        }
      >
        {loading && !overview ? (
          <div className="empty">
            <Spinner size={22} label="Chargement" />
          </div>
        ) : (
          <dl className="admin-stats">
            <Stat label="Cours" value={overview?.courses ?? 0} />
            <Stat label="Sections" value={overview?.sections ?? 0} />
            <Stat label="Machines" value={overview?.boxes ?? 0} />
            <Stat label="Comptes" value={overview?.users ?? 0} />
            <Stat label="Flags validés" value={overview?.flagsValidated ?? 0} />
            <Stat label="Sections terminées" value={overview?.sectionsCompleted ?? 0} />
          </dl>
        )}
      </Panel>

      <Panel title="À savoir">
        <ul className="admin-notes">
          <li>
            Un cours publié ici apparaît aussitôt dans la filière choisie, sans redémarrage ni rechargement du
            catalogue.
          </li>
          <li>
            Renommer une section, la déplacer ou lui ajouter une vidéo n'efface l'avancement de personne. La
            retirer du cours, en revanche, supprime l'avancement qui s'y rapportait.
          </li>
          <li>
            Les machines du catalogue et leurs flags restent semés au premier démarrage du serveur : ils ne se
            gèrent pas encore depuis cette page.
          </li>
        </ul>
      </Panel>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <div>
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  );
}
