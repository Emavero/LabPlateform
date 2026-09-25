import { DIFFICULTY_ORDER, difficultyLevel, type Difficulty } from '@/domain/models/Box';

interface DifficultyMeterProps {
  difficulty: Difficulty;
  label: string;
}

/** Jauge à cinq barres : lecture immédiate du niveau, comme une note. */
export function DifficultyMeter({ difficulty, label }: DifficultyMeterProps) {
  const level = difficultyLevel(difficulty);
  return (
    <span className={`difficulty difficulty--${difficulty.toLowerCase()}`}>
      <span className="difficulty__bars" role="img" aria-label={`Difficulté : ${label}`}>
        {DIFFICULTY_ORDER.map((_, index) => (
          <span key={index} className={['difficulty__bar', index < level && 'difficulty__bar--on'].filter(Boolean).join(' ')} />
        ))}
      </span>
      <span className="difficulty__label">{label}</span>
    </span>
  );
}
