import { Link } from 'react-router-dom';
import type { Scenario } from '@/domain/models/Scenario';
import { Icon } from '../../design-system';
import { useI18n } from '../../i18n/I18nContext';

/**
 * Étapes d'un scénario, dans l'ordre.
 * <p>
 * Une étape franchie porte une coche et un mot, pas seulement une couleur. Une
 * étape dont la ressource a disparu est signalée plutôt que masquée : un
 * scénario qui bloque doit se voir, sinon le joueur croit que la plateforme est
 * cassée.
 */
export function ScenarioSteps({ scenario }: { scenario: Scenario }) {
  const { t } = useI18n();

  return (
    <ol className="steps">
      {scenario.steps.map((step) => (
        <li
          key={step.id ?? step.position}
          className={['steps__item', step.done && 'steps__item--done'].filter(Boolean).join(' ')}
        >
          <span className="steps__marker" aria-hidden="true">
            {step.done ? <Icon name="check" size={14} /> : step.position}
          </span>
          <span className="steps__body">
            <span className="steps__head">
              <span className="steps__name">
                {step.missing || step.locked ? (
                  step.name
                ) : (
                  <Link to={step.kind === 'MACHINE' ? `/machines/${step.reference}` : `/cours/forensique/${step.reference}`}>
                    {step.name}
                  </Link>
                )}
              </span>
              <span className="steps__kind">{t(`scenario.kind.${step.kind}`)}</span>
              <span className={['steps__state', step.done && 'steps__state--done'].filter(Boolean).join(' ')}>
                {t(step.done ? 'scenario.done' : 'scenario.todo')}
              </span>
            </span>
            {step.instruction && <span className="steps__instruction">{step.instruction}</span>}
            <span className="steps__meta">
              {step.objective && <span>{t(`scenario.objective.${step.objective}`)}</span>}
              {step.locked && (
                <span className="badge badge--locked">
                  <Icon name="lock" size={12} /> {t('scenario.locked')}
                </span>
              )}
              {step.missing && <span className="steps__missing">{t('scenario.missing')}</span>}
            </span>
          </span>
        </li>
      ))}
    </ol>
  );
}
