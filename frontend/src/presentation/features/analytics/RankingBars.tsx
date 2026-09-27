import { barWidth, type ContentInsight } from '@/domain/models/Analytics';
import { useI18n } from '../../i18n/I18nContext';

/**
 * Classement de contenus, en barres horizontales.
 * <p>
 * Une seule teinte pour toutes les barres : la longueur porte déjà la
 * comparaison, et colorer chaque ligne différemment reviendrait à encoder deux
 * fois la même information — en gaspillant le seul canal qui restait libre.
 * L'horizontale est choisie parce que les intitulés sont des noms de machines
 * et de cours, qui ne tiennent pas sous une colonne.
 */
export function RankingBars({ insight }: { insight: ContentInsight }) {
  const { t } = useI18n();
  if (insight.entries.length === 0) {
    return <p className="empty">{t('admin.noData')}</p>;
  }

  return (
    <ul className="ranking" aria-label={insight.title}>
      {insight.entries.map((entry) => (
        <li key={entry.subject} className="ranking__row">
          <span className="ranking__label" title={entry.subject}>
            {entry.subject}
          </span>
          <span className="ranking__track">
            <span className="ranking__bar" style={{ width: `${barWidth(entry.count, insight.entries)}%` }} />
          </span>
          <span className="ranking__value">
            {entry.count}
            <span className="ranking__unit"> {insight.unit}</span>
          </span>
        </li>
      ))}
    </ul>
  );
}
