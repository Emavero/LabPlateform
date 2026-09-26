import { DIFFICULTY_ORDER, type Box, type Difficulty, ratingGap } from '@/domain/models/Box';
import { Icon } from '../../design-system';

const LABELS: Record<Difficulty, string> = {
  VERY_EASY: 'Très facile',
  EASY: 'Facile',
  MEDIUM: 'Moyenne',
  HARD: 'Difficile',
  INSANE: 'Insane',
};

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
  const gap = ratingGap(box);

  return (
    <div className="rating">
      <p className="rating__summary">
        {box.ratingVotes === 0 ? (
          <>Personne n'a encore noté cette machine.</>
        ) : (
          <>
            {box.ratingVotes} vote{box.ratingVotes > 1 ? 's' : ''} · ressentie{' '}
            <strong>{box.perceivedDifficultyName}</strong>
            {gap !== 0 && (
              <span className="rating__gap">
                {gap > 0 ? ' plus dure qu’annoncé' : ' plus facile qu’annoncé'}
              </span>
            )}
          </>
        )}
      </p>

      {box.pwned ? (
        <div className="rating__options" role="group" aria-label="Noter la difficulté">
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
              {LABELS[difficulty]}
            </button>
          ))}
        </div>
      ) : (
        <p className="rating__locked">
          <Icon name="lock" size={14} /> Notez cette machine une fois ses deux flags validés.
        </p>
      )}
    </div>
  );
}
