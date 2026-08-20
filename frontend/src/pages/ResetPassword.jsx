import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { AuthLayout } from '../components/AuthLayout';
import { FormField } from '../components/FormField';
import { authApi } from '../api/authApi';

export function ResetPassword() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const [token, setToken] = useState(searchParams.get('token') || '');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  const validate = () => {
    const errors = {};
    if (!token.trim()) {
      errors.token = 'Le token est obligatoire.';
    }
    if (newPassword.length < 8) {
      errors.newPassword = 'Le mot de passe doit contenir au moins 8 caractères.';
    }
    if (newPassword !== confirmPassword) {
      errors.confirmPassword = 'Les mots de passe ne correspondent pas.';
    }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setServerError('');
    if (!validate()) return;

    setLoading(true);
    try {
      await authApi.resetPassword({ token, newPassword, confirmPassword });
      setSuccess(true);
    } catch (err) {
      setServerError(err.response?.data?.message || 'Échec de la réinitialisation.');
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <AuthLayout>
        <div className="card">
          <h2>Mot de passe mis à jour</h2>
          <p className="card__subtitle">Vous pouvez maintenant vous connecter avec votre nouveau mot de passe.</p>
          <button className="btn-primary" onClick={() => navigate('/login')}>
            Aller à la connexion
          </button>
        </div>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout>
      <div className="card">
        <h2>Réinitialiser le mot de passe</h2>
        <p className="card__subtitle">Collez le token reçu et choisissez un nouveau mot de passe.</p>

        {serverError && <div className="alert-error">{serverError}</div>}

        <form onSubmit={handleSubmit} noValidate>
          <FormField
            label="Token de réinitialisation"
            value={token}
            onChange={(e) => setToken(e.target.value)}
            error={fieldErrors.token}
            placeholder="Collé depuis l'e-mail ou l'étape précédente"
            required
          />
          <FormField
            label="Nouveau mot de passe"
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            error={fieldErrors.newPassword}
            placeholder="8 caractères minimum"
            required
          />
          <FormField
            label="Confirmation du mot de passe"
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            error={fieldErrors.confirmPassword}
            placeholder="••••••••"
            required
          />

          <button className="btn-primary" type="submit" disabled={loading}>
            {loading ? 'Mise à jour…' : 'Réinitialiser le mot de passe'}
          </button>
        </form>

        <div className="card__switch">
          <Link to="/login">← Retour à la connexion</Link>
        </div>
      </div>
    </AuthLayout>
  );
}
