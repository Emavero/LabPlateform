import { useCallback, useEffect, useState, type ChangeEvent, type FormEvent } from 'react';
import type { UserProfile } from '@/domain/models/User';
import { displayNameOf, initialsOf, ROLE_LABELS } from '@/domain/models/User';
import type { PasswordChange } from '@/domain/repositories/AccountRepository';
import { PASSWORD_MIN_LENGTH } from '@/domain/validation/credentials';
import { Alert, Avatar, Button, Panel, TextField } from '../design-system';
import { formatDate } from '../features/lab/format';
import { useAction } from '../hooks/useAction';
import { useDependencies } from '../state/DependenciesContext';

const EMPTY: PasswordChange = { currentPassword: '', newPassword: '', confirmPassword: '' };

export function SettingsPage() {
  const { account } = useDependencies();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [form, setForm] = useState<PasswordChange>(EMPTY);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    account.getProfile.execute().then(setProfile, () => undefined);
  }, [account.getProfile]);

  const change = useCallback(
    (value: PasswordChange) => account.changePassword.execute(value).then(() => true),
    [account.changePassword],
  );
  const { run, pending, error, fieldErrors } = useAction(change);

  const update = (key: keyof PasswordChange) => (e: ChangeEvent<HTMLInputElement>) => {
    setSaved(false);
    setForm((f) => ({ ...f, [key]: e.target.value }));
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (await run(form)) {
      setForm(EMPTY);
      setSaved(true);
    }
  };

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <h1 className="page__title">Paramètres du compte</h1>
        </div>
      </header>

      <div className="settings-grid">
        <Panel title="Profil">
          {profile ? (
            <div className="profile">
              <Avatar initials={initialsOf(profile)} size="lg" />
              <dl className="profile__list">
                <div>
                  <dt>Nom affiché</dt>
                  <dd>{displayNameOf(profile)}</dd>
                </div>
                <div>
                  <dt>Adresse e-mail</dt>
                  <dd>{profile.email}</dd>
                </div>
                <div>
                  <dt>Rôle</dt>
                  <dd>{ROLE_LABELS[profile.role]}</dd>
                </div>
                <div>
                  <dt>Membre depuis</dt>
                  <dd>{formatDate(profile.createdAt)}</dd>
                </div>
              </dl>
            </div>
          ) : (
            <p className="muted">Chargement du profil…</p>
          )}
        </Panel>

        <Panel title="Mot de passe" description="Les sessions ouvertes restent valides jusqu'à leur expiration.">
          {saved && <Alert tone="success">Votre mot de passe a été modifié.</Alert>}
          {error && Object.keys(fieldErrors).length === 0 && <Alert tone="error">{error.message}</Alert>}
          <form className="form" onSubmit={submit} noValidate>
            <TextField
              label="Mot de passe actuel"
              type="password"
              autoComplete="current-password"
              value={form.currentPassword}
              onChange={update('currentPassword')}
              error={fieldErrors.currentPassword}
              revealable
            />
            <TextField
              label="Nouveau mot de passe"
              type="password"
              autoComplete="new-password"
              value={form.newPassword}
              onChange={update('newPassword')}
              error={fieldErrors.newPassword}
              hint={`Au moins ${PASSWORD_MIN_LENGTH} caractères.`}
              revealable
            />
            <TextField
              label="Confirmation"
              type="password"
              autoComplete="new-password"
              value={form.confirmPassword}
              onChange={update('confirmPassword')}
              error={fieldErrors.confirmPassword}
              revealable
            />
            <Button type="submit" loading={pending} loadingLabel="Enregistrement…">
              Modifier le mot de passe
            </Button>
          </form>
        </Panel>
      </div>
    </div>
  );
}
