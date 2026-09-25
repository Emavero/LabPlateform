import { useState, type ChangeEvent, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { PASSWORD_MIN_LENGTH } from '@/domain/validation/credentials';
import { Alert, Button, TextField } from '../design-system';
import { useAction } from '../hooks/useAction';
import { AuthLayout } from '../layouts/AuthLayout';
import { useAuth } from '../state/AuthContext';

export function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: '', password: '', confirmPassword: '' });
  const { run, pending, error, fieldErrors } = useAction(register);

  const update = (key: keyof typeof form) => (e: ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [key]: e.target.value }));

  // Le serveur signale une adresse déjà utilisée par un 409 : on l'affiche sous le champ concerné.
  const emailTaken = error?.kind === 'conflict';

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (await run(form)) navigate('/', { replace: true });
  };

  return (
    <AuthLayout
      title="Créer un compte"
      subtitle="Deux machines de lab, Windows et Linux, vous attendent."
      footer={
        <>
          Déjà inscrit ? <Link to="/login">Se connecter</Link>
        </>
      }
    >
      {error && !emailTaken && Object.keys(fieldErrors).length === 0 && <Alert tone="error">{error.message}</Alert>}
      <form className="form" onSubmit={submit} noValidate>
        <TextField
          label="Adresse e-mail"
          type="email"
          icon="mail"
          autoComplete="email"
          value={form.email}
          onChange={update('email')}
          error={fieldErrors.email ?? (emailTaken ? error?.message : undefined)}
          autoFocus
        />
        <TextField
          label="Mot de passe"
          type="password"
          icon="lock"
          autoComplete="new-password"
          value={form.password}
          onChange={update('password')}
          error={fieldErrors.password}
          hint={`Au moins ${PASSWORD_MIN_LENGTH} caractères.`}
          revealable
        />
        <TextField
          label="Confirmation du mot de passe"
          type="password"
          icon="lock"
          autoComplete="new-password"
          value={form.confirmPassword}
          onChange={update('confirmPassword')}
          error={fieldErrors.confirmPassword}
          revealable
        />
        <Button type="submit" block loading={pending} loadingLabel="Création…">
          Créer mon compte
        </Button>
      </form>
    </AuthLayout>
  );
}
