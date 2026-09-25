import { useCallback, useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import type { PasswordReset } from '@/domain/repositories/AuthRepository';
import { PASSWORD_MIN_LENGTH } from '@/domain/validation/credentials';
import { Alert, Button, TextField } from '../design-system';
import { useAction } from '../hooks/useAction';
import { AuthLayout } from '../layouts/AuthLayout';
import { useDependencies } from '../state/DependenciesContext';

export function ResetPasswordPage() {
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
      title="Nouveau mot de passe"
      subtitle="Choisissez un mot de passe que vous n'utilisez nulle part ailleurs."
      footer={<Link to="/login">Retour à la connexion</Link>}
    >
      {done ? (
        <Alert
          tone="success"
          title="Mot de passe mis à jour"
          action={
            <Link className="btn btn--primary btn--sm" to="/login">
              Se connecter
            </Link>
          }
        >
          Vous pouvez vous connecter avec votre nouveau mot de passe.
        </Alert>
      ) : (
        <>
          {!token && <Alert tone="error">Ce lien de réinitialisation est incomplet. Refaites une demande.</Alert>}
          {globalError && <Alert tone="error">{globalError}</Alert>}
          <form className="form" onSubmit={submit} noValidate>
            <TextField
              label="Nouveau mot de passe"
              type="password"
              icon="lock"
              autoComplete="new-password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              error={fieldErrors.newPassword}
              hint={`Au moins ${PASSWORD_MIN_LENGTH} caractères.`}
              revealable
              autoFocus
            />
            <TextField
              label="Confirmation"
              type="password"
              icon="lock"
              autoComplete="new-password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              error={fieldErrors.confirmPassword}
              revealable
            />
            <Button type="submit" block loading={pending} loadingLabel="Enregistrement…" disabled={!token}>
              Enregistrer
            </Button>
          </form>
        </>
      )}
    </AuthLayout>
  );
}
