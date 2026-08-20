import { Link } from 'react-router-dom';

export function NotFound() {
  return (
    <div className="not-found">
      <div className="not-found__code">ERR_404 · ROUTE_NOT_FOUND</div>
      <h1>Aucune ressource à ce chemin</h1>
      <p>La page demandée n'existe pas ou a été déplacée.</p>
      <Link className="btn-primary not-found__link" to="/">
        Retour au tableau de bord
      </Link>
    </div>
  );
}
