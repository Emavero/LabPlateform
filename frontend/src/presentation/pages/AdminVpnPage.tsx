import { useCallback, useEffect, useRef, useState, type ChangeEvent } from 'react';
import { formatProfileSize, type LabVpnProfile } from '@/domain/models/Vpn';
import { toAppError, type AppError } from '@/domain/errors/AppError';
import { Alert, Button, Icon, Panel, Spinner } from '../design-system';
import { useI18n } from '../i18n/I18nContext';
import { useDependencies } from '../state/DependenciesContext';

/**
 * Le profil VPN que les apprenants téléchargeront.
 * <p>
 * Un seul fichier pour toute la plateforme : le déposer remplace le précédent,
 * le retirer coupe le téléchargement pour tout le monde. C'est dit sur l'écran,
 * parce que c'est la différence qui compte avec l'autre mode — celui où la
 * plateforme émet un certificat par personne, révocable individuellement.
 */
export function AdminVpnPage() {
  const { t, formatDateTime } = useI18n();
  const { admin } = useDependencies();
  const input = useRef<HTMLInputElement>(null);

  const [profile, setProfile] = useState<LabVpnProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<AppError | null>(null);
  const [justSaved, setJustSaved] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setProfile(await admin.getVpnProfile.execute());
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setLoading(false);
    }
  }, [admin.getVpnProfile]);

  useEffect(() => {
    void load();
  }, [load]);

  async function onPick(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    // Le champ est remis à zéro tout de suite : sans cela, redéposer le même
    // fichier après une correction ne déclencherait aucun événement.
    event.target.value = '';
    if (!file) return;
    setBusy(true);
    setError(null);
    setJustSaved(false);
    try {
      setProfile(await admin.uploadVpnProfile.execute(file));
      setJustSaved(true);
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setBusy(false);
    }
  }

  async function onRemove() {
    setBusy(true);
    setError(null);
    setJustSaved(false);
    try {
      await admin.removeVpnProfile.execute();
      await load();
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="page__eyebrow">{t('admin.eyebrow')}</p>
          <h1 className="page__title">{t('adminVpn.title')}</h1>
          <p className="page__lead">{t('adminVpn.lead')}</p>
        </div>
      </header>

      {error && <Alert tone="error">{error.message}</Alert>}

      <Panel title={t('adminVpn.current')} description={t('adminVpn.currentHint')}>
        {loading ? (
          <div className="empty">
            <Spinner size={20} label={t('common.loading')} />
          </div>
        ) : profile?.present ? (
          <>
            {justSaved && <Alert tone="success">{t('adminVpn.saved')}</Alert>}

            {/* L'avertissement le plus utile de l'écran : sans route, le tunnel
                monte et les cibles ne répondent pas. */}
            {!profile.routesLabNetwork && (
              <Alert tone="info" title={t('adminVpn.noRouteTitle')}>
                {t('adminVpn.noRouteText')}
              </Alert>
            )}

            <dl className="deposited">
              <div>
                <dt>{t('adminVpn.fileName')}</dt>
                <dd className="deposited__name">
                  <Icon name="vpn" size={15} /> {profile.fileName}
                </dd>
              </div>
              <div>
                <dt>{t('adminVpn.size')}</dt>
                <dd>{formatProfileSize(profile.sizeBytes)}</dd>
              </div>
              <div>
                <dt>{t('adminVpn.uploadedAt')}</dt>
                <dd>{profile.uploadedAt ? formatDateTime(profile.uploadedAt) : '—'}</dd>
              </div>
            </dl>
          </>
        ) : (
          <p className="empty">{t('adminVpn.none')}</p>
        )}

        <div className="editor-footer">
          {/* Le champ natif est masqué : son apparence n'est pas réglable, et
              un bouton ordinaire se décrit mieux. */}
          <input
            ref={input}
            className="visually-hidden"
            type="file"
            accept=".ovpn"
            onChange={(event) => void onPick(event)}
          />
          <Button
            variant={profile?.present ? 'ghost' : 'primary'}
            icon="upload"
            loading={busy}
            loadingLabel={t('adminVpn.uploading')}
            onClick={() => input.current?.click()}
          >
            {t(profile?.present ? 'adminVpn.replace' : 'adminVpn.upload')}
          </Button>
          {profile?.present && (
            <Button variant="ghost" icon="trash" disabled={busy} onClick={() => void onRemove()}>
              {t('adminVpn.remove')}
            </Button>
          )}
        </div>
      </Panel>

      <Panel title={t('adminVpn.howTitle')}>
        <ol className="schema__list adminVpn__steps">
          <li>{t('adminVpn.how1')}</li>
          <li>{t('adminVpn.how2')}</li>
          <li>{t('adminVpn.how3')}</li>
        </ol>
        <p className="schema__caption">
          <Icon name="info" size={14} /> {t('adminVpn.shared')}
        </p>
      </Panel>
    </div>
  );
}
