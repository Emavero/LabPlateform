import { useMemo, useState } from 'react';
import {
  familiesOf,
  filterByFamily,
  groupByDay,
  type JournalFamily,
  type JournalLine,
} from '@/domain/models/Journal';
import { Icon, type IconName } from '../../design-system';
import { useI18n } from '../../i18n/I18nContext';

const FAMILY_ICONS: Record<JournalFamily, IconName> = {
  ACCOUNT: 'user',
  MACHINES: 'target',
  ACADEMY: 'book',
  LAB: 'server',
  BILLING: 'crown',
  SUPPORT: 'support',
};

interface JournalTimelineProps {
  lines: readonly JournalLine[];
  /** Vrai sur la vue d'administration : chaque ligne nomme alors son auteur. */
  showAuthor?: boolean;
}

/**
 * Journal présenté par journée et filtrable par famille. Une liste plate de
 * deux cents lignes ne se lit pas ; groupée par jour, elle raconte quelque chose.
 */
export function JournalTimeline({ lines, showAuthor = false }: JournalTimelineProps) {
  const { t, locale } = useI18n();
  const [family, setFamily] = useState<JournalFamily | null>(null);
  const families = useMemo(() => familiesOf(lines), [lines]);
  const days = useMemo(() => groupByDay(filterByFamily(lines, family)), [lines, family]);

  if (lines.length === 0) {
    return <p className="empty">{t('journal.empty')}</p>;
  }

  return (
    <div className="journal">
      <div className="journal__filters" role="group" aria-label={t('journal.filterGroup')}>
        <button
          type="button"
          className={['journal__filter', family === null && 'journal__filter--active'].filter(Boolean).join(' ')}
          aria-pressed={family === null}
          onClick={() => setFamily(null)}
        >
          {t('journal.filterAll')}
        </button>
        {families.map((candidate) => (
          <button
            key={candidate}
            type="button"
            className={['journal__filter', family === candidate && 'journal__filter--active']
              .filter(Boolean)
              .join(' ')}
            aria-pressed={family === candidate}
            onClick={() => setFamily(candidate)}
          >
            <Icon name={FAMILY_ICONS[candidate]} size={14} />
            {lines.find((line) => line.family === candidate)?.familyName ?? candidate}
          </button>
        ))}
      </div>

      {days.map(({ day, lines: ofDay }) => (
        <section key={day} className="journal__day">
          <h3 className="journal__date">{new Date(day).toLocaleDateString(locale, { dateStyle: 'long' })}</h3>
          <ul className="journal__list">
            {ofDay.map((line, index) => (
              <li key={`${line.at.toISOString()}-${index}`} className="journal__line">
                <span className={`journal__icon journal__icon--${line.family.toLowerCase()}`}>
                  <Icon name={FAMILY_ICONS[line.family]} size={15} />
                </span>
                <span className="journal__text">
                  <span className="journal__kind">{line.kindName}</span>
                  {line.subject && <code className="journal__subject">{line.subject}</code>}
                  {line.detail && <span className="journal__detail">{line.detail}</span>}
                </span>
                {showAuthor && <span className="journal__author">{line.handle}</span>}
                <time className="journal__time" dateTime={line.at.toISOString()}>
                  {line.at.toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' })}
                </time>
              </li>
            ))}
          </ul>
        </section>
      ))}
    </div>
  );
}
