import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Alert, Button, TextField } from '../design-system';
import { useI18n } from '../i18n/I18nContext';
import { useAction } from '../hooks/useAction';
import { AuthLayout } from '../layouts/AuthLayout';
import type { RedirectState } from '../routing/guards';
import { useAuth } from '../state/AuthContext';

export function LoginPage() {
  const { t } = useI18n();
  const { login, sessionExpired } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as RedirectState | null)?.from ?? '/';
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const { run, pending, error, fieldErrors } = useAction(login);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (await run({ email, password })) navigate(from, { replace: true });
  };

  return (
    <AuthLayout
      title={t('auth.signIn')}
      subtitle={t('auth.signInLead')}
      footer={
        <>
          {t('auth.noAccount')} <Link to="/register">{t('auth.createAccount')}</Link>
        </>
      }
    >
      {sessionExpired && !error && <Alert tone="info">{t('auth.expired')}</Alert>}
      {error && Object.keys(fieldErrors).length === 0 && <Alert tone="error">{error.message}</Alert>}
      <form className="form" onSubmit={submit} noValidate>
        <TextField
          label={t('auth.email')}
          type="email"
          icon="mail"
          autoComplete="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          error={fieldErrors.email}
          autoFocus
        />
        <TextField
          label={t('auth.password')}
          type="password"
          icon="lock"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={fieldErrors.password}
          revealable
        />
        <div className="form__row-end">
          <Link className="text-link" to="/forgot-password">
            {t('auth.forgot')}
          </Link>
        </div>
        <Button type="submit" block loading={pending} loadingLabel={t('auth.signingIn')}>
          {t('auth.signInAction')}
        </Button>
      </form>
    </AuthLayout>
  );
}
