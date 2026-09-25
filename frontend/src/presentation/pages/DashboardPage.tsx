import { Link } from 'react-router-dom';
import { displayNameOf } from '@/domain/models/User';
import { isRunning } from '@/domain/models/VirtualMachine';
import { Alert, Button, Icon, Panel, Spinner } from '../design-system';
import { VmCard } from '../features/lab/VmCard';
import { useLab } from '../hooks/useLab';
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
  const running = lab.vms.filter(isRunning).length;

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
