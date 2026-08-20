import { useEffect, useState } from 'react';
import { AppLayout } from '../components/AppLayout';
import { FormField } from '../components/FormField';
import { userApi } from '../api/userApi';

export function Profile() {
  const [profile, setProfile] = useState(null);
  const [loadingProfile, setLoadingProfile] = useState(true);

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    userApi
      .getProfile()
      .then(setProfile)
      .catch(() => setServerError('Impossible de charger votre profil.'))
      .finally(() => setLoadingProfile(false));
  }, []);

  const validate = () => {
    const errors = {};
    if (!currentPassword) errors.currentPassword = 'Le mot de passe actuel est obligatoire.';
    if (newPassword.length < 8) errors.newPassword = 'Le mot de passe doit contenir au moins 8 caractères.';
    if (newPassword !== confirmPassword) errors.confirmPassword = 'Les mots de passe ne correspondent pas.';
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };


  const handleSubmit = async (e) => {
    e.preventDefault();
    setServerError('');
    setSuccessMessage('');
    if (!validate()) return;

    setSaving(true);
    try {
      await userApi.changePassword({ currentPassword, newPassword, confirmPassword });
      setSuccessMessage('Mot de passe mis à jour avec succès.');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      setServerError(err.response?.data?.message || 'Échec de la mise à jour du mot de passe.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <AppLayout>
      <div className="page__header">
        <h1>Mon profil</h1>
        <p>Gérez les informations de votre compte.</p>
      </div>

      {loadingProfile ? (
        <div className="loading-text">Chargement du profil…</div>
      ) : (
        <div className="profile-layout">
          <section className="card profile-card">
            <h2>Informations du compte</h2>
            <div className="profile-info">
              <div className="profile-info__row">
                <span>Adresse e-mail</span>
                <span className="mono">{profile?.email}</span>
              </div>
              <div className="profile-info__row">
                <span>Rôle</span>
                <span className="mono">{profile?.role}</span>
              </div>
              <div className="profile-info__row">
                <span>Membre depuis</span>
                <span className="mono">
                  {profile?.createdAt ? new Date(profile.createdAt).toLocaleDateString('fr-FR') : '—'}
                </span>
              </div>
            </div>
          </section>

          <section className="card profile-card">
            <h2>Changer le mot de passe</h2>

            {serverError && <div className="alert-error">{serverError}</div>}
            {successMessage && <div className="alert-success">{successMessage}</div>}

            <form onSubmit={handleSubmit} noValidate>
              <FormField
                label="Mot de passe actuel"
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                error={fieldErrors.currentPassword}
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
                label="Confirmation du nouveau mot de passe"
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                error={fieldErrors.confirmPassword}
                required
              />

              <button className="btn-primary" type="submit" disabled={saving}>
                {saving ? 'Mise à jour…' : 'Mettre à jour le mot de passe'}
              </button>
            </form>
          </section>
        </div>
      )}
    </AppLayout>
  );
}
