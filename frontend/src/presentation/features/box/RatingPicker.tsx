import { DIFFICULTY_ORDER, type Box, type Difficulty, ratingGap } from '@/domain/models/Box';
import { Icon } from '../../design-system';
import { useI18n } from '../../i18n/I18nContext';

interface RatingPickerProps {
  box: Box;
  pending: boolean;
  onRate: (difficulty: Difficulty) => void;
}

/**
 * Difficulté ressentie par les joueurs, à côté de celle annoncée. Le vote
 * n'est proposé qu'aux machines possédées : avant, on n'a pas vu la moitié
 * du travail.
 */
export function RatingPicker({ box, pending, onRate }: RatingPickerProps) {
  const { t } = useI18n();
  const gap = ratingGap(box);

  return (
    <div className="rating">
      <p className="rating__summary">
        {box.ratingVotes === 0 ? (
          <>{t('rating.none')}</>
        ) : (
          <>
            {t('rating.votes', { count: box.ratingVotes })} <strong>{box.perceivedDifficultyName}</strong>
            {gap !== 0 && (
              <span className="rating__gap">{t(gap > 0 ? 'rating.harder' : 'rating.easier')}</span>
            )}
          </>
        )}
      </p>

      {box.pwned ? (
        <div className="rating__options" role="group" aria-label={t('rating.group')}>
          {DIFFICULTY_ORDER.map((difficulty) => (
            <button
              key={difficulty}
              type="button"
              className={['rating__option', box.myRating === difficulty && 'rating__option--active']
                .filter(Boolean)
                .join(' ')}
              aria-pressed={box.myRating === difficulty}
              disabled={pending}
              onClick={() => onRate(difficulty)}
            >
              {t(`difficulty.${difficulty}`)}
            </button>
          ))}
        </div>
      ) : (
        <p className="rating__locked">
          <Icon name="lock" size={14} /> {t('rating.locked')}
        </p>
      )}
    </div>
  );
}
