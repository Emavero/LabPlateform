import { Link } from 'react-router-dom';
import { Alert, Button } from '../design-system';
import { useVpnAccess } from '../hooks/useVpn';
import { useLab } from '../hooks/useLab';
import { LabGrid } from './DashboardPage';

export function LabsPage() {
  const lab = useLab();
  const vpn = useVpnAccess();
  return (
    <div className="page">
      <header className="page__header">
        <div>
          <h1 className="page__title">Lab Infrastructure</h1>
          <p className="page__lead">
            Une machine Windows (bureau à distance) et une machine Linux (SSH), dédiées à votre compte.
          </p>
        </div>
        <Button variant="ghost" size="sm" icon="refresh" loading={lab.loading} onClick={() => void lab.reload()}>
          Actualiser
        </Button>
      </header>
      {vpn.access?.enabled && (
        <Alert
          tone="info"
          title="Accès par le VPN du lab"
          action={
            <Link className="btn btn--ghost btn--sm" to="/vpn">
              VPN Access
            </Link>
          }
        >
          Connectez-vous au VPN ({vpn.access.labNetwork}) avant de joindre une machine en SSH ou en RDP.
        </Alert>
      )}
      <LabGrid lab={lab} />
    </div>
  );
}
