import { useState } from 'react';
import { Link } from 'react-router-dom';
import { AuthLayout } from '../components/AuthLayout';
import { FormField } from '../components/FormField';
import { authApi } from '../api/authApi';

export function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState('');
  const [devToken, setDevToken] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setDevToken('');
    setLoading(true);
    try {
      const response = await authApi.forgotPassword(email);
      setMessage(response.message);
      if (response.devResetToken) {
        setDevToken(response.devResetToken);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Une erreur est survenue.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout>
      <div className="card">
        <h2>Mot de passe oublié</h2>
        <p className="card__subtitle">
          Indiquez votre adresse e-mail, nous générerons un lien de réinitialisation
        </p>

        {error && <div className="alert-error">{error}</div>}
        {message && <div className="alert-success">{message}</div>}

        {devToken && (
          <div className="dev-token-box">
            <div className="dev-token-box__label">
              Mode développement — aucun serveur d'e-mail n'est branché.Utilisez ce token de test :
            </div>
            <div className="dev-token-box__value mono">{devToken}</div>
            <Link className="dev-token-box__cta" to={`/reset-password?token=${encodeURIComponent(devToken)}`}>
              Réinitialiser mon mot de passe
            </Link>
          </div>
        )}

        {!message && (
          <form onSubmit={handleSubmit}>
            <FormField
              label="Adresse e-mail"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="vous@exemple.com"
              required
            />
            <button className="btn-primary" type="submit" disabled={loading}>
              {loading ? 'Envoi…' : 'Envoyer le lien de réinitialisation'}
            </button>
          </form>
        )}

        <div className="card__switch">
          <Link to="/login">Retour à la connexion</Link>
        </div>
      </div>
    </AuthLayout>
  );
}
