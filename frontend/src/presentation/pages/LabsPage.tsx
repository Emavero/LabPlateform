import { Link } from 'react-router-dom';
import { Alert, Button } from '../design-system';
import { useVpnAccess } from '../hooks/useVpn';
import { useI18n } from '../i18n/I18nContext';
import { useLab } from '../hooks/useLab';
import { LabGrid } from './DashboardPage';

export function LabsPage() {
  const { t } = useI18n();
  const lab = useLab();
  const vpn = useVpnAccess();
  return (
    <div className="page">
      <header className="page__header">
        <div>
          <h1 className="page__title">{t('nav.labs')}</h1>
          <p className="page__lead">{t('labs.lead2')}</p>
        </div>
        <Button variant="ghost" size="sm" icon="refresh" loading={lab.loading} onClick={() => void lab.reload()}>
          {t('common.refresh')}
        </Button>
      </header>
      {vpn.access?.enabled && (
        <Alert
          tone="info"
          title={t('labs.vpnTitle')}
          action={
            <Link className="btn btn--ghost btn--sm" to="/vpn">
              {t('nav.vpn')}
            </Link>
          }
        >
          {t('labs.vpnText', { network: vpn.access.labNetwork })}
        </Alert>
      )}
      <LabGrid lab={lab} />
    </div>
  );
}
