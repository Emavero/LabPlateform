import type { RealCase } from '@/domain/models/Course';
import { Panel } from '../../design-system';
import { useI18n } from '../../i18n/I18nContext';

/**
 * Cas d'usage réel : la même matière, vue depuis un métier.
 * <p>
 * Quatre intitulés fixes plutôt qu'un texte libre : ils obligent l'auteur à
 * dire l'enjeu et l'issue, qui sont précisément ce qu'un récit d'incident omet
 * quand on le laisse libre.
 */
export function RealCasePanel({ id, realCase }: { id: string; realCase: RealCase }) {
  const { t } = useI18n();

  return (
    <Panel
      id={id}
      className="real-case"
      eyebrow={t('course.realCaseEyebrow')}
      title={t('course.realCase')}
      description={realCase.sector}
    >
      <dl className="real-case__grid">
        <div>
          <dt>{t('course.caseSituation')}</dt>
          <dd>{realCase.situation}</dd>
        </div>
        <div>
          <dt>{t('course.caseStake')}</dt>
          <dd>{realCase.stake}</dd>
        </div>
        <div>
          <dt>{t('course.caseOutcome')}</dt>
          <dd>{realCase.outcome}</dd>
        </div>
      </dl>
    </Panel>
  );
}
