import { useCallback, useEffect, useId, useState } from 'react';
import { Link } from 'react-router-dom';
import { defaultProtocol, VPN_PROTOCOL_LABELS, type VpnAccess, type VpnProtocol } from '@/domain/models/Vpn';
import { Alert, Button, CopyField, Icon, Panel, Spinner } from '../design-system';
import { formatDate } from '../features/lab/format';
import { CONNECTION_GUIDES, detectClientOs, type ClientOs } from '../features/vpn/connectionGuides';
import { saveTextFile } from '../features/vpn/saveFile';
import { useAction } from '../hooks/useAction';
import { useVpnAccess } from '../hooks/useVpn';
import { useDependencies } from '../state/DependenciesContext';

const PROTOCOL_HINTS: Record<VpnProtocol, string> = {
  udp: 'Recommandé, plus rapide',
  tcp: 'Si votre réseau bloque l’UDP',
};

export function VpnPage() {
  const vpn = useVpnAccess();

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <h1 className="page__title">VPN Access</h1>
          <p className="page__lead">
            Connectez votre ordinateur au réseau du lab pour joindre vos machines depuis vos propres outils.
          </p>
        </div>
      </header>

      {vpn.loading && !vpn.access ? (
        <div className="empty">
          <Spinner size={22} label="Chargement de l'accès VPN" />
        </div>
      ) : vpn.loadError ? (
        <Alert
          tone="error"
          title="Impossible de charger l'accès VPN"
          action={
            <Button variant="ghost" size="sm" icon="refresh" onClick={() => void vpn.reload()}>
              Réessayer
            </Button>
          }
        >
          {vpn.loadError.message}
        </Alert>
      ) : vpn.access && !vpn.access.enabled ? (
        <Alert tone="info" title="Accès VPN non configuré">
          L'administrateur n'a pas encore activé le VPN sur cette plateforme.
        </Alert>
      ) : vpn.access ? (
        <VpnAccessView access={vpn.access} onChange={vpn.setAccess} onDownloaded={() => void vpn.reload()} />
      ) : null}
    </div>
  );
}

interface VpnAccessViewProps {
  access: VpnAccess;
  onChange: (access: VpnAccess) => void;
  onDownloaded: () => void;
}

function VpnAccessView({ access, onChange, onDownloaded }: VpnAccessViewProps) {
  const { vpn } = useDependencies();
  const [protocol, setProtocol] = useState<VpnProtocol | null>(() => defaultProtocol(access));
  const [confirming, setConfirming] = useState(false);
  const [regenerated, setRegenerated] = useState(false);
  const endpoint = access.endpoints.find((e) => e.protocol === protocol);
  const fileName = `cyberMans-lab-${protocol ?? 'udp'}.ovpn`;

  const download = useAction(
    useCallback(async (chosen: VpnProtocol) => {
      const file = await vpn.download.execute(chosen);
      saveTextFile(file.fileName, file.content);
      return true;
    }, [vpn.download]),
  );
  const regenerate = useAction(useCallback(() => vpn.regenerate.execute(), [vpn.regenerate]));

  const onDownload = async () => {
    if (!protocol) return;
    setRegenerated(false);
    if (await download.run(protocol)) onDownloaded();
  };

  const onRegenerate = async () => {
    const updated = await regenerate.run();
    setConfirming(false);
    if (updated) {
      onChange(updated);
      setRegenerated(true);
    }
  };

  return (
    <div className="vpn-grid">
      <Panel
        title="Votre profil VPN"
        description="Un fichier personnel : il vous identifie sur le réseau du lab. Ne le partagez pas."
        className="vpn-profile"
      >
        <div className={['vpn-status', access.issuedAt && 'vpn-status--ready'].filter(Boolean).join(' ')}>
          <Icon name="vpn" size={22} />
          <div>
            <p className="vpn-status__title">{access.issuedAt ? 'Profil actif' : 'Aucun profil pour le moment'}</p>
            <p className="vpn-status__text">
              {access.issuedAt
                ? `Généré le ${formatDate(access.issuedAt)}.`
                : 'Il sera créé automatiquement à votre premier téléchargement.'}
            </p>
          </div>
        </div>

        <ProtocolPicker access={access} value={protocol} onChange={setProtocol} />

        <dl className="vpn-facts">
          <div>
            <dt>Serveur</dt>
            <dd>{endpoint ? `${endpoint.host}:${endpoint.port}` : '—'}</dd>
          </div>
          <div>
            <dt>Réseau du lab</dt>
            <dd>{access.labNetwork}</dd>
          </div>
        </dl>

        {download.error && <Alert tone="error">{download.error.message}</Alert>}
        {regenerated && (
          <Alert tone="success" title="Nouveau profil généré">
            L'ancien fichier ne fonctionne plus. Téléchargez le nouveau et remplacez-le dans votre client VPN.
          </Alert>
        )}

        <Button icon="download" block loading={download.pending} loadingLabel="Préparation…" onClick={onDownload} disabled={!protocol}>
          Télécharger le profil {protocol ? VPN_PROTOCOL_LABELS[protocol] : ''}
        </Button>

        <div className="vpn-regenerate">
          {confirming ? (
            <Alert
              tone="error"
              title="Régénérer le profil ?"
              action={
                <div className="vpn-regenerate__actions">
                  <Button variant="ghost" size="sm" onClick={() => setConfirming(false)} disabled={regenerate.pending}>
                    Annuler
                  </Button>
                  <Button variant="danger" size="sm" loading={regenerate.pending} onClick={onRegenerate}>
                    Régénérer
                  </Button>
                </div>
              }
            >
              Votre fichier actuel cessera immédiatement de fonctionner, sur tous vos appareils.
            </Alert>
          ) : (
            <button type="button" className="text-link vpn-regenerate__trigger" onClick={() => setConfirming(true)}>
              <Icon name="refresh" size={14} /> Régénérer mon profil (fichier perdu ou partagé par erreur)
            </button>
          )}
          {regenerate.error && <Alert tone="error">{regenerate.error.message}</Alert>}
        </div>
      </Panel>

      <ConnectionGuidePanel fileName={fileName} />
    </div>
  );
}

function ProtocolPicker({
  access,
  value,
  onChange,
}: {
  access: VpnAccess;
  value: VpnProtocol | null;
  onChange: (protocol: VpnProtocol) => void;
}) {
  const labelId = useId();
  return (
    <div className="segmented" role="radiogroup" aria-labelledby={labelId}>
      <span className="segmented__label" id={labelId}>
        Protocole
      </span>
      <div className="segmented__options">
        {(['udp', 'tcp'] as const).map((protocol) => {
          const available = access.endpoints.some((e) => e.protocol === protocol);
          const checked = value === protocol;
          return (
            <button
              key={protocol}
              type="button"
              role="radio"
              aria-checked={checked}
              disabled={!available}
              className={['segmented__option', checked && 'segmented__option--active'].filter(Boolean).join(' ')}
              onClick={() => onChange(protocol)}
            >
              <span className="segmented__name">{VPN_PROTOCOL_LABELS[protocol]}</span>
              <span className="segmented__hint">{available ? PROTOCOL_HINTS[protocol] : 'Non proposé'}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}

function ConnectionGuidePanel({ fileName }: { fileName: string }) {
  const [os, setOs] = useState<ClientOs>('linux');
  const tabsId = useId();
  useEffect(() => setOs(detectClientOs()), []);
  const guide = CONNECTION_GUIDES[os];

  return (
    <Panel title="Se connecter" description="Suivez les étapes correspondant à votre système.">
      <div className="tabs" role="tablist" aria-label="Système d'exploitation">
        {(Object.keys(CONNECTION_GUIDES) as ClientOs[]).map((key) => (
          <button
            key={key}
            type="button"
            role="tab"
            id={`${tabsId}-${key}`}
            aria-selected={os === key}
            aria-controls={`${tabsId}-panel`}
            className={['tabs__tab', os === key && 'tabs__tab--active'].filter(Boolean).join(' ')}
            onClick={() => setOs(key)}
          >
            {CONNECTION_GUIDES[key].label}
          </button>
        ))}
      </div>
      <div role="tabpanel" id={`${tabsId}-panel`} aria-labelledby={`${tabsId}-${os}`} className="tabs__panel">
        {guide.command && (
          <div className="terminal">
            <div className="terminal__bar">
              <span className="terminal__dots" aria-hidden="true">
                <i />
                <i />
                <i />
              </span>
              <span className="terminal__title">
                <Icon name="terminal" size={14} /> Terminal
              </span>
            </div>
            <div className="terminal__body">
              <CopyField label="Commande" value={guide.command(fileName)} />
            </div>
          </div>
        )}
        <ol className="access__steps">
          {guide.steps(fileName).map((step) => (
            <li key={step}>{step}</li>
          ))}
        </ol>
        <Alert tone="info" title="Et ensuite ?">
          Une fois connecté, démarrez une machine dans <Link to="/labs">Lab Infrastructure</Link> et utilisez l'adresse
          affichée sur sa carte.
        </Alert>
      </div>
    </Panel>
  );
}
