import { Link } from 'react-router-dom';
import type { Scenario } from '@/domain/models/Scenario';
import { Icon } from '../../design-system';
import { useI18n } from '../../i18n/I18nContext';

/**
 * Scénario et son avancement.
 * <p>
 * L'avancement se lit par la fraction, la barre et, quand il est complet, par
 * une mention explicite : trois lectures pour une seule information, parce que
 * la barre seule ne dit pas si le dernier pas a été franchi.
 */
export function ScenarioCard({ scenario }: { scenario: Scenario }) {
  const { t } = useI18n();
  const next = scenario.steps.find((step) => step.position === scenario.progress.nextPosition);

  return (
    <article className="scenario">
      <header className="scenario__head">
        <h3 className="scenario__title">
          <Link to={`/modules/scenario-designer/${scenario.slug}`}>{scenario.title}</Link>
        </h3>
        <p className="scenario__count">{t('scenario.progress', { done: scenario.progress.done, total: scenario.progress.total })}</p>
      </header>

      <div
        className="scenario__gauge"
        role="img"
        aria-label={t('scenario.percentDone', { percent: scenario.progress.percent })}
      >
        <span className="scenario__bar" style={{ width: `${Math.max(2, scenario.progress.percent)}%` }} />
      </div>

      <p className="scenario__brief">{scenario.brief}</p>

      {scenario.progress.complete ? (
        <p className="scenario__state scenario__state--done">
          <Icon name="check" size={14} /> {t('scenario.complete')}
        </p>
      ) : (
        next && (
          <p className="scenario__state">
            <Icon name="target" size={14} /> {t('scenario.next', { name: next.name })}
          </p>
        )
      )}
    </article>
  );
}
