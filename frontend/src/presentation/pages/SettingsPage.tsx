import { useCallback, useEffect, useState, type ChangeEvent, type FormEvent } from 'react';
import type { UserProfile } from '@/domain/models/User';
import { displayNameOf, initialsOf } from '@/domain/models/User';
import type { PasswordChange } from '@/domain/repositories/AccountRepository';
import { PASSWORD_MIN_LENGTH } from '@/domain/validation/credentials';
import { Alert, Avatar, Button, Panel, TextField } from '../design-system';
import { useI18n } from '../i18n/I18nContext';
import { LanguagePicker } from '../i18n/LanguagePicker';
import { useAction } from '../hooks/useAction';
import { useDependencies } from '../state/DependenciesContext';

const EMPTY: PasswordChange = { currentPassword: '', newPassword: '', confirmPassword: '' };

export function SettingsPage() {
  const { t, formatDate } = useI18n();
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
          <h1 className="page__title">{t('settings.title')}</h1>
        </div>
      </header>

      <div className="settings-grid">
        <Panel title={t('settings.language')} description={t('settings.languageHint')}>
          <LanguagePicker />
        </Panel>

        <Panel title={t('settings.profile')}>
          {profile ? (
            <div className="profile">
              <Avatar initials={initialsOf(profile)} size="lg" />
              <dl className="profile__list">
                <div>
                  <dt>{t('settings.displayName')}</dt>
                  <dd>{displayNameOf(profile)}</dd>
                </div>
                <div>
                  <dt>{t('auth.email')}</dt>
                  <dd>{profile.email}</dd>
                </div>
                <div>
                  <dt>{t('settings.role')}</dt>
                  <dd>{t(`role.${profile.role}`)}</dd>
                </div>
                <div>
                  <dt>{t('settings.memberSince')}</dt>
                  <dd>{formatDate(profile.createdAt)}</dd>
                </div>
              </dl>
            </div>
          ) : (
            <p className="muted">{t('settings.loadingProfile')}</p>
          )}
        </Panel>

        <Panel title={t('settings.password')} description={t('settings.passwordHint')}>
          {saved && <Alert tone="success">{t('settings.passwordChanged')}</Alert>}
          {error && Object.keys(fieldErrors).length === 0 && <Alert tone="error">{error.message}</Alert>}
          <form className="form" onSubmit={submit} noValidate>
            <TextField
              label={t('settings.currentPassword')}
              type="password"
              autoComplete="current-password"
              value={form.currentPassword}
              onChange={update('currentPassword')}
              error={fieldErrors.currentPassword}
              revealable
            />
            <TextField
              label={t('auth.newPassword')}
              type="password"
              autoComplete="new-password"
              value={form.newPassword}
              onChange={update('newPassword')}
              error={fieldErrors.newPassword}
              hint={t('auth.passwordHint', { count: PASSWORD_MIN_LENGTH })}
              revealable
            />
            <TextField
              label={t('auth.confirmPassword')}
              type="password"
              autoComplete="new-password"
              value={form.confirmPassword}
              onChange={update('confirmPassword')}
              error={fieldErrors.confirmPassword}
              revealable
            />
            <Button type="submit" loading={pending} loadingLabel={t('writeup.saving')}>
              {t('settings.changePassword')}
            </Button>
          </form>
        </Panel>
      </div>
    </div>
  );
}
