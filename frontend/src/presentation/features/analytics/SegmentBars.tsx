import type { UsageSegment } from '@/domain/models/Analytics';

/**
 * Répartition des comptes par profil d'usage.
 * <p>
 * Réponse à « qui sont mes utilisateurs ? » quand leur nombre ne dit rien : un
 * compte qui attaque des machines et un compte qui suit des cours n'attendent
 * pas la même chose. Chaque ligne porte donc aussi ce qu'il y a à faire pour
 * cette classe.
 * <p>
 * Barres d'une seule teinte, et non un camembert : cinq parts proches se
 * comparent mal en angles, et les intitulés n'y tiennent pas.
 */
export function SegmentBars({ segments }: { segments: readonly UsageSegment[] }) {
  const total = segments.reduce((sum, segment) => sum + segment.accounts, 0);
  if (total === 0) {
    return <p className="empty">Aucun compte à classer pour l’instant.</p>;
  }

  return (
    <ul className="segments">
      {segments
        .filter((segment) => segment.accounts > 0)
        .map((segment) => (
          <li key={segment.profile} className="segments__row">
            <div className="segments__head">
              <span className="segments__name">{segment.profileName}</span>
              <span className="segments__count">
                {segment.accounts} compte{segment.accounts > 1 ? 's' : ''} · {segment.share} %
              </span>
            </div>
            <span className="segments__track">
              <span className="segments__bar" style={{ width: `${segment.share}%` }} />
            </span>
            <p className="segments__advice">{segment.advice}</p>
          </li>
        ))}
    </ul>
  );
}
