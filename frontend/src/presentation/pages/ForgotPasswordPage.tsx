import { useCallback, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import type { PasswordResetRequestResult } from '@/domain/repositories/AuthRepository';
import { Alert, Button, TextField } from '../design-system';
import { useI18n } from '../i18n/I18nContext';
import { useAction } from '../hooks/useAction';
import { AuthLayout } from '../layouts/AuthLayout';
import { useDependencies } from '../state/DependenciesContext';

export function ForgotPasswordPage() {
  const { t } = useI18n();
  const { auth } = useDependencies();
  const [email, setEmail] = useState('');
  const [result, setResult] = useState<PasswordResetRequestResult | null>(null);
  const request = useCallback((value: string) => auth.requestPasswordReset.execute(value), [auth.requestPasswordReset]);
  const { run, pending, error, fieldErrors } = useAction(request);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    const outcome = await run(email);
    if (outcome) setResult(outcome);
  };

  return (
    <AuthLayout
      title={t('auth.forgotTitle')}
      subtitle={t('auth.forgotLeadLong')}
      footer={<Link to="/login">{t('auth.backToSignIn')}</Link>}
    >
      {result ? (
        <>
          <Alert tone="success">{result.message}</Alert>
          {result.demoToken && (
            <Alert
              tone="info"
              title={t('auth.demoMode')}
              action={
                <Link className="btn btn--ghost btn--sm" to={`/reset-password?token=${encodeURIComponent(result.demoToken)}`}>
                  {t('auth.demoAction')}
                </Link>
              }
            >
              {t('auth.demoText')}
            </Alert>
          )}
        </>
      ) : (
        <>
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
            <Button type="submit" block loading={pending} loadingLabel={t('auth.forgotSending')}>
              {t('auth.forgotAction')}
            </Button>
          </form>
        </>
      )}
    </AuthLayout>
  );
}
