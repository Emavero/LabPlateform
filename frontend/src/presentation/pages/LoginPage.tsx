import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Alert, Button, TextField } from '../design-system';
import { useAction } from '../hooks/useAction';
import { AuthLayout } from '../layouts/AuthLayout';
import type { RedirectState } from '../routing/guards';
import { useAuth } from '../state/AuthContext';

export function LoginPage() {
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
      title="Connexion"
      subtitle="Accédez à vos environnements de lab."
      footer={
        <>
          Pas encore de compte ? <Link to="/register">Créer un compte</Link>
        </>
      }
    >
      {sessionExpired && !error && <Alert tone="info">Votre session a expiré. Reconnectez-vous.</Alert>}
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
        <TextField
          label="Mot de passe"
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
            Mot de passe oublié ?
          </Link>
        </div>
        <Button type="submit" block loading={pending} loadingLabel="Connexion…">
          Se connecter
        </Button>
      </form>
    </AuthLayout>
  );
}
