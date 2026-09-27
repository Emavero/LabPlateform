import type { Recommendation, RecommendationSeverity } from '@/domain/models/Analytics';
import { Icon, type IconName } from '../../design-system';

/**
 * L'icône et le mot portent la gravité autant que la couleur : un niveau qui ne
 * se lirait qu'à la teinte serait invisible pour une partie des lecteurs.
 */
const SEVERITY_ICONS: Record<RecommendationSeverity, IconName> = {
  WARNING: 'alert',
  OPPORTUNITY: 'target',
  INFO: 'info',
};

/**
 * Ce qu'il y a à faire, déduit de ce que les comptes ont réellement fait.
 * <p>
 * Chaque conseil affiche le chiffre qui le motive : un tableau de bord qui
 * conseille sans montrer sa preuve n'est pas vérifiable, donc pas crédible.
 */
export function RecommendationList({ recommendations }: { recommendations: readonly Recommendation[] }) {
  if (recommendations.length === 0) {
    return <p className="empty">Rien à signaler sur la période.</p>;
  }

  return (
    <ul className="advice">
      {recommendations.map((recommendation) => (
        <li
          key={recommendation.code + (recommendation.subject ?? '')}
          className={`advice__item advice__item--${recommendation.severity.toLowerCase()}`}
        >
          <span className="advice__icon">
            <Icon name={SEVERITY_ICONS[recommendation.severity]} size={16} />
          </span>
          <div className="advice__body">
            <p className="advice__title">
              {recommendation.title}
              <span className="advice__severity">{recommendation.severityName}</span>
            </p>
            <p className="advice__text">{recommendation.advice}</p>
            <p className="advice__evidence">
              {recommendation.subject && <code>{recommendation.subject}</code>}
              {recommendation.evidence}
            </p>
          </div>
        </li>
      ))}
    </ul>
  );
}
