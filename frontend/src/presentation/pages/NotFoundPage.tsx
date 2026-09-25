import { Link } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <div className="page page--center">
      <p className="page__eyebrow">Erreur 404</p>
      <h1 className="page__title">Page introuvable</h1>
      <p className="page__lead">Cette ressource n'existe pas ou ne vous est pas accessible.</p>
      <Link className="btn btn--primary btn--md" to="/">
        Retour au tableau de bord
      </Link>
    </div>
  );
}
