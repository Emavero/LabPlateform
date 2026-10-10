import { useCallback, useEffect, useId, useState } from 'react';
import { Link } from 'react-router-dom';
import { defaultProtocol, isUploadedSource, type VpnAccess, type VpnProtocol } from '@/domain/models/Vpn';
import { Alert, Button, CopyField, Icon, Panel, Spinner } from '../design-system';
import { CONNECTION_GUIDES, detectClientOs, type ClientOs } from '../features/vpn/connectionGuides';
import { saveTextFile } from '../features/vpn/saveFile';
import { useI18n } from '../i18n/I18nContext';
import { useAction } from '../hooks/useAction';
import { useVpnAccess } from '../hooks/useVpn';
import { useDependencies } from '../state/DependenciesContext';

export function VpnPage() {
  const { t } = useI18n();
  const vpn = useVpnAccess();

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <h1 className="page__title">{t('vpn.title')}</h1>
          <p className="page__lead">{t('vpn.lead')}</p>
        </div>
      </header>

      {vpn.loading && !vpn.access ? (
        <div className="empty">
          <Spinner size={22} label={t('vpn.loading')} />
        </div>
      ) : vpn.loadError ? (
        <Alert
          tone="error"
          title={t('vpn.loadError')}
          action={
            <Button variant="ghost" size="sm" icon="refresh" onClick={() => void vpn.reload()}>
              {t('common.retry')}
            </Button>
          }
        >
          {vpn.loadError.message}
        </Alert>
      ) : vpn.access && !vpn.access.enabled ? (
        <Alert tone="info" title={t('vpn.offTitle')}>
          {t('vpn.offText')}
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
  const { t, formatDate } = useI18n();
  const { vpn } = useDependencies();
  const [protocol, setProtocol] = useState<VpnProtocol | null>(() => defaultProtocol(access));
  const [confirming, setConfirming] = useState(false);
  const [regenerated, setRegenerated] = useState(false);
  const endpoint = access.endpoints.find((e) => e.protocol === protocol);
  const fileName = `cyberMans-lab-${protocol ?? 'udp'}.ovpn`;
  /*
   * Profil déposé par l'administration : un seul fichier, le même pour tous.
   * Il n'y a alors ni protocole à choisir — le fichier porte le sien — ni
   * régénération possible, puisque personne n'émet rien.
   */
  const uploaded = isUploadedSource(access);

  const download = useAction(
    useCallback(async (chosen: VpnProtocol) => {
      const file = await vpn.download.execute(chosen);
      saveTextFile(file.fileName, file.content);
      return true;
    }, [vpn.download]),
  );
  const regenerate = useAction(useCallback(() => vpn.regenerate.execute(), [vpn.regenerate]));

  const onDownload = async () => {
    // En source déposée il n'y a qu'un fichier : le protocole est ignoré par
    // le serveur, et en exiger un ici empêcherait tout téléchargement.
    const chosen = protocol ?? 'udp';
    if (!uploaded && !protocol) return;
    setRegenerated(false);
    if (await download.run(chosen)) onDownloaded();
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
        title={t('vpn.profile')}
        description={t('vpn.profileHint')}
        className="vpn-profile"
      >
        <div className={['vpn-status', access.issuedAt && 'vpn-status--ready'].filter(Boolean).join(' ')}>
          <Icon name="vpn" size={22} />
          <div>
            <p className="vpn-status__title">{t(access.issuedAt ? 'vpn.profileActive' : 'vpn.profileNone')}</p>
            <p className="vpn-status__text">
              {access.issuedAt
                ? t('vpn.generatedOn', { date: formatDate(access.issuedAt) })
                : t('vpn.willBeCreated')}
            </p>
          </div>
        </div>

        {!uploaded && <ProtocolPicker access={access} value={protocol} onChange={setProtocol} />}

        <dl className="vpn-facts">
          {!uploaded && (
            <div>
              <dt>{t('vpn.server')}</dt>
              <dd>{endpoint ? `${endpoint.host}:${endpoint.port}` : '—'}</dd>
            </div>
          )}
          <div>
            <dt>{t('vpn.labNetwork')}</dt>
            <dd>{access.labNetwork}</dd>
          </div>
        </dl>

        {download.error && <Alert tone="error">{download.error.message}</Alert>}
        {regenerated && (
          <Alert tone="success" title={t('vpn.regenerated')}>
            {t('vpn.regeneratedText')}
          </Alert>
        )}

        <Button
          icon="download"
          block
          loading={download.pending}
          loadingLabel={t('vpn.preparing')}
          onClick={onDownload}
          disabled={!uploaded && !protocol}
        >
          {uploaded
            ? t('vpn.downloadShared')
            : t('vpn.download', { protocol: protocol ? t(`vpn.protocol.${protocol}`) : '' })}
        </Button>

        {/* Rien à régénérer sur un fichier qu'on n'a pas émis. */}
        {!uploaded && <div className="vpn-regenerate">
          {confirming ? (
            <Alert
              tone="error"
              title={t('vpn.regenerateTitle')}
              action={
                <div className="vpn-regenerate__actions">
                  <Button variant="ghost" size="sm" onClick={() => setConfirming(false)} disabled={regenerate.pending}>
                    {t('common.cancel')}
                  </Button>
                  <Button variant="danger" size="sm" loading={regenerate.pending} onClick={onRegenerate}>
                    {t('vpn.regenerate')}
                  </Button>
                </div>
              }
            >
              {t('vpn.regenerateWarning')}
            </Alert>
          ) : (
            <button type="button" className="text-link vpn-regenerate__trigger" onClick={() => setConfirming(true)}>
              <Icon name="refresh" size={14} /> {t('vpn.regenerateTrigger')}
            </button>
          )}
          {regenerate.error && <Alert tone="error">{regenerate.error.message}</Alert>}
        </div>}
      </Panel>

      <ConnectionGuidePanel fileName={uploaded ? t('vpn.sharedFileName') : fileName} />
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
  const { t } = useI18n();
  const labelId = useId();
  return (
    <div className="segmented" role="radiogroup" aria-labelledby={labelId}>
      <span className="segmented__label" id={labelId}>
        {t('vpn.protocol')}
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
              <span className="segmented__name">{t(`vpn.protocol.${protocol}`)}</span>
              <span className="segmented__hint">
                {available ? t(`vpn.hint.${protocol}`) : t('vpn.notOffered')}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}

function ConnectionGuidePanel({ fileName }: { fileName: string }) {
  const { t } = useI18n();
  const [os, setOs] = useState<ClientOs>('linux');
  const tabsId = useId();
  useEffect(() => setOs(detectClientOs()), []);
  const guide = CONNECTION_GUIDES[os];

  return (
    <Panel title={t('vpn.connect')} description={t('vpn.connectHint')}>
      <div className="tabs" role="tablist" aria-label={t('vpn.osGroup')}>
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
              <CopyField label={t('vpn.command')} value={guide.command(fileName)} />
            </div>
          </div>
        )}
        <ol className="access__steps">
          {guide.steps.map((step) => (
            <li key={step}>{t(step, { file: fileName })}</li>
          ))}
        </ol>
        <Alert tone="info" title={t('vpn.next')}>
          {t('vpn.nextBefore')} <Link to="/machines">{t('nav.machines')}</Link> {t('vpn.nextAfter')}
        </Alert>
      </div>
    </Panel>
  );
}
