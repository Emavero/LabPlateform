import { useCallback, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import type { PasswordResetRequestResult } from '@/domain/repositories/AuthRepository';
import { Alert, Button, TextField } from '../design-system';
import { useAction } from '../hooks/useAction';
import { AuthLayout } from '../layouts/AuthLayout';
import { useDependencies } from '../state/DependenciesContext';

export function ForgotPasswordPage() {
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
      title="Mot de passe oublié"
      subtitle="Indiquez votre adresse : si un compte existe, un lien de réinitialisation vous sera envoyé."
      footer={<Link to="/login">Retour à la connexion</Link>}
    >
      {result ? (
        <>
          <Alert tone="success">{result.message}</Alert>
          {result.demoToken && (
            <Alert
              tone="info"
              title="Mode démonstration"
              action={
                <Link className="btn btn--ghost btn--sm" to={`/reset-password?token=${encodeURIComponent(result.demoToken)}`}>
                  Réinitialiser
                </Link>
              }
            >
              Aucun serveur d'e-mail n'est configuré : le lien est fourni directement.
            </Alert>
          )}
        </>
      ) : (
        <>
          {error && Object.keys(fieldErrors).length === 0 && <Alert tone="error">{error.message}</Alert>}
          <form className="form" onSubmit={submit} noValidate>
            <TextField
              label="Adresse e-mail"
              type="email"
              icon="mail"
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              error={fieldErrors.email}
              autoFocus
            />
            <Button type="submit" block loading={pending} loadingLabel="Envoi…">
              Envoyer le lien
            </Button>
          </form>
        </>
      )}
    </AuthLayout>
  );
}
