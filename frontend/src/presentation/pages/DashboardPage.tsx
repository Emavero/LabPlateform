import { Link } from 'react-router-dom';
import { displayNameOf } from '@/domain/models/User';
import { isRunning } from '@/domain/models/VirtualMachine';
import { Alert, Button, Icon, Panel, Spinner } from '../design-system';
import { BoxCard } from '../features/box/BoxCard';
import { ProgressPanel } from '../features/box/ProgressPanel';
import { VmCard } from '../features/lab/VmCard';
import { useBoxes } from '../hooks/useBoxes';
import { useLab } from '../hooks/useLab';
import { useProgress } from '../hooks/useProgress';
import { useAuth } from '../state/AuthContext';

function greeting(now = new Date()): string {
  const hour = now.getHours();
  if (hour < 5 || hour >= 18) return 'Bonsoir';
  return 'Bonjour';
}

function summary(loading: boolean, total: number, running: number): string {
  if (loading) return 'Chargement de votre environnement…';
  if (total === 0) return 'Aucune machine provisionnée pour le moment.';
  if (running === 0) return `Vos ${total} machines sont arrêtées.`;
  return `${running} machine${running > 1 ? 's' : ''} sur ${total} en cours d'exécution.`;
}

export function DashboardPage() {
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
            {greeting()}, {user ? displayNameOf(user) : ''}
          </h1>
          <p className="page__lead">{summary(lab.loading, lab.vms.length, running)}</p>
        </div>
      </header>

      <Panel
        title="Progression"
        description="Votre rang, vos points et les machines qu'il vous reste à posséder."
        actions={
          <Link className="btn btn--ghost btn--sm" to="/scoreboard">
            <Icon name="trophy" size={16} />
            <span>Classement</span>
          </Link>
        }
      >
        <ProgressPanel progress={progress} />
      </Panel>

      <Panel
        title="Machines à compromettre"
        description="Les cibles du moment. Trouvez leurs deux flags pour marquer leurs points."
        actions={
          <Link className="btn btn--ghost btn--sm" to="/machines">
            <Icon name="target" size={16} />
            <span>Tout le catalogue</span>
          </Link>
        }
      >
        {catalogue.loading && catalogue.boxes.length === 0 ? (
          <div className="empty">
            <Spinner size={22} label="Chargement des machines" />
          </div>
        ) : featured.length === 0 ? (
          <p className="empty">
            {catalogue.boxes.length === 0
              ? "Aucune machine n'est publiée pour le moment."
              : 'Toutes les machines du catalogue sont possédées. Chapeau.'}
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
        title="Lab / Infrastructure"
        description="Démarrez une machine pour afficher son adresse, son port et ses identifiants temporaires."
        actions={
          <Link className="btn btn--ghost btn--sm" to="/labs">
            <Icon name="server" size={16} />
            <span>Ouvrir le lab</span>
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
  if (lab.loading && lab.vms.length === 0) {
    return (
      <div className="empty">
        <Spinner size={22} label="Chargement des machines" />
      </div>
    );
  }
  if (lab.loadError) {
    return (
      <Alert
        tone="error"
        title="Impossible de charger les machines"
        action={
          <Button variant="ghost" size="sm" icon="refresh" onClick={() => void lab.reload()}>
            Réessayer
          </Button>
        }
      >
        {lab.loadError.message}
      </Alert>
    );
  }
  if (lab.vms.length === 0) {
    return <p className="empty">Aucune machine n'est encore provisionnée pour votre compte.</p>;
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
