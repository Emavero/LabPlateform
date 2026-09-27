import { useCallback, useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import type { PasswordReset } from '@/domain/repositories/AuthRepository';
import { PASSWORD_MIN_LENGTH } from '@/domain/validation/credentials';
import { Alert, Button, TextField } from '../design-system';
import { useI18n } from '../i18n/I18nContext';
import { useAction } from '../hooks/useAction';
import { AuthLayout } from '../layouts/AuthLayout';
import { useDependencies } from '../state/DependenciesContext';

export function ResetPasswordPage() {
  const { t } = useI18n();
  const { auth } = useDependencies();
  const [params] = useSearchParams();
  const token = params.get('token') ?? '';
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [done, setDone] = useState(false);
  const reset = useCallback(
    (value: PasswordReset) => auth.resetPassword.execute(value).then(() => true),
    [auth.resetPassword],
  );
  const { run, pending, error, fieldErrors } = useAction(reset);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (await run({ token, newPassword, confirmPassword })) setDone(true);
  };

  const globalError = error && (fieldErrors.token ?? (Object.keys(fieldErrors).length === 0 ? error.message : null));

  return (
    <AuthLayout
      title={t('auth.resetTitle')}
      subtitle={t('auth.resetLeadLong')}
      footer={<Link to="/login">{t('auth.backToSignIn')}</Link>}
    >
      {done ? (
        <Alert
          tone="success"
          title={t('auth.resetDoneTitle')}
          action={
            <Link className="btn btn--primary btn--sm" to="/login">
              {t('auth.signInAction')}
            </Link>
          }
        >
          {t('auth.resetDoneText')}
        </Alert>
      ) : (
        <>
          {!token && <Alert tone="error">{t('auth.resetMissing')}</Alert>}
          {globalError && <Alert tone="error">{globalError}</Alert>}
          <form className="form" onSubmit={submit} noValidate>
            <TextField
              label={t('auth.newPassword')}
              type="password"
              icon="lock"
              autoComplete="new-password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              error={fieldErrors.newPassword}
              hint={t('auth.passwordHint', { count: PASSWORD_MIN_LENGTH })}
              revealable
              autoFocus
            />
            <TextField
              label={t('auth.confirmPassword')}
              type="password"
              icon="lock"
              autoComplete="new-password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              error={fieldErrors.confirmPassword}
              revealable
            />
            <Button type="submit" block loading={pending} loadingLabel={t('auth.resetting')} disabled={!token}>
              {t('common.save')}
            </Button>
          </form>
        </>
      )}
    </AuthLayout>
  );
}
