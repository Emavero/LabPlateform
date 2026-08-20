import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AuthLayout } from '../components/AuthLayout';
import { FormField } from '../components/FormField';
import { useAuth } from '../context/AuthContext';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [loading, setLoading] = useState(false);

  const validate = () => {
    const errors = {};

    if (!EMAIL_REGEX.test(email)) {
      errors.email = "Le format de l'adresse e-mail est invalide";
    }
    if (password.length < 8) {
      errors.password = 'Le mot de passe doit contenir au moins 8 caractères';
    }
    if (password !== confirmPassword) {
      errors.confirmPassword = 'Les mots de passe ne correspondent pas';
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
      await register(email, password, confirmPassword);
      navigate('/');
    } catch (err) {
      const message = err.response?.data?.message || "Échec de l'inscription. Réessayez.";
      setServerError(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout>
      <div className="card">
        <h2>Créer un compte</h2>
        <p className="card__subtitle">Inscrivez-vous pour accéder à vos labs.</p>

        {serverError && <div className="alert-error">{serverError}</div>}

        <form onSubmit={handleSubmit} noValidate>
          <FormField
            label="Adresse e-mail"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            error={fieldErrors.email}
            placeholder="vous@exemple.com"
            required
          />
          <FormField
            label="Mot de passe"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            error={fieldErrors.password}
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
            {loading ? 'Création…' : 'Créer mon compte'}
          </button>
        </form>

        <div className="card__switch">
          Déjà inscrit ? <Link to="/login">Se connecter</Link>
        </div>
      </div>
    </AuthLayout>
  );
}
