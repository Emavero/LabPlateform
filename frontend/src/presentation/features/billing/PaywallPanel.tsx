import { Link } from 'react-router-dom';
import type { Box } from '@/domain/models/Box';
import { Icon, Panel } from '../../design-system';

/**
 * Ce que voit un compte gratuit à la place des informations d'attaque.
 * <p>
 * Le panneau dit ce qui manque et pourquoi, plutôt que d'afficher un refus :
 * la fiche de la machine reste consultable, c'est l'adresse, la cible à la
 * demande et les comptes rendus qui sont derrière l'abonnement.
 */
export function PaywallPanel({ box }: { box: Box }) {
  return (
    <Panel
      title="Machine réservée aux abonnés Pro"
      description={`${box.name} · ${box.difficultyName} · ${box.totalPoints} points à gagner`}
    >
      <ul className="paywall__list">
        <li>
          <Icon name="lock" size={16} /> Adresse de la cible dans le réseau du lab
        </li>
        <li>
          <Icon name="lock" size={16} /> Lancement d'une cible à la demande, pour vous seul
        </li>
        <li>
          <Icon name="lock" size={16} /> Soumission des deux flags et points au classement
        </li>
        <li>
          <Icon name="lock" size={16} /> Comptes rendus des joueurs qui l'ont possédée
        </li>
      </ul>
      <p className="paywall__note">
        Les cours, le classement et les machines d'initiation restent accessibles sans abonnement.
      </p>
      <Link className="btn btn--primary btn--sm" to="/abonnement">
        Voir les formules
      </Link>
    </Panel>
  );
}
