import { useNavigate } from 'react-router-dom';
import { AppLayout } from '../components/AppLayout';

/**
 * Page d'accueil post-connexion. Le panneau "Lab" est le premier module
 * exposé ; d'autres panneaux (quotas, historique, notifications...)
 * pourront être ajoutés ici sans toucher au reste de l'application.
 */
export function Dashboard() {
  const navigate = useNavigate();

  return (
    <AppLayout>
      <div className="page__header">
        <h1>Tableau de bord</h1>
        <p>Accédez à vos modules disponibles</p>
      </div>

      <div className="panel-grid">
        <button className="panel-card" onClick={() => navigate('/labs')}>
          <div className="panel-card__icon">[ vm ]</div>
          <h3>Lab</h3>
          <p>Démarrez et gérez vos machines virtuelles Windows et Linux</p>
        </button>
      </div>
    </AppLayout>
  );
}
