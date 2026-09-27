import { Link } from 'react-router-dom';
import type { Box } from '@/domain/models/Box';
import { Icon } from '../../design-system';
import { useI18n } from '../../i18n/I18nContext';
import { DifficultyMeter } from './DifficultyMeter';
import { FlagChip } from './FlagChip';

/** Vignette du catalogue : ce qu'il faut pour choisir sa prochaine cible. */
export function BoxCard({ box }: { box: Box }) {
  const { t } = useI18n();
  const titleId = `box-${box.slug}-title`;
  return (
    <article
      className={['box-card', box.pwned && 'box-card--pwned', box.locked && 'box-card--locked']
        .filter(Boolean)
        .join(' ')}
      aria-labelledby={titleId}
    >
      <header className="box-card__header">
        <span className={`box-card__os box-card__os--${box.os.toLowerCase()}`}>
          <Icon name={box.os === 'WINDOWS' ? 'windows' : 'linux'} size={24} />
        </span>
        <div className="box-card__heading">
          <p className="box-card__family">{t(`os.${box.os}`)}</p>
          <h3 className="box-card__name" id={titleId}>
            <Link to={`/machines/${box.slug}`}>{box.name}</Link>
          </h3>
        </div>
        <div className="box-card__badges">
          {box.pwned && (
            <span className="badge badge--pwned">
              <Icon name="check" size={13} /> {t('boxes.owned')}
            </span>
          )}
          {box.firstBlood && (
            <span className="badge badge--blood">
              <Icon name="crown" size={13} /> {t('boxes.firstBlood')}
            </span>
          )}
          {box.locked && (
            <span className="badge badge--locked">
              <Icon name="lock" size={13} /> {t('boxes.lockedBadge')}
            </span>
          )}
          {box.retired && <span className="badge">{t('boxes.retired')}</span>}
        </div>
      </header>

      <p className="box-card__synopsis">
        {box.locked ? t('boxes.locked') : box.synopsis}
      </p>

      <div className="box-card__meta">
        <DifficultyMeter difficulty={box.difficulty} label={box.difficultyName} />
        <span className="box-card__points">
          {box.pointsEarned > 0 ? `${box.pointsEarned} / ${box.totalPoints}` : box.totalPoints} pts
        </span>
      </div>

      <div className="box-card__flags">
        <FlagChip kind="USER" owned={box.userOwned} points={box.userFlagPoints} />
        <FlagChip kind="ROOT" owned={box.rootOwned} points={box.rootFlagPoints} />
      </div>

      <footer className="box-card__footer">
        {box.locked ? (
          <span className="box-card__ip box-card__ip--hidden">
            <Icon name="lock" size={13} /> {t('boxes.hiddenAddress')}
          </span>
        ) : (
          <code className="box-card__ip">{box.ipAddress}</code>
        )}
        <Link className="text-link" to={`/machines/${box.slug}`}>
          {t(box.locked ? 'boxes.unlock' : 'boxes.attack')} <Icon name="chevronRight" size={14} />
        </Link>
      </footer>
    </article>
  );
}
