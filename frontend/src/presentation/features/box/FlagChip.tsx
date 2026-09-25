import { FLAG_LABELS, type FlagKind } from '@/domain/models/Box';
import { Icon } from '../../design-system';

interface FlagChipProps {
  kind: FlagKind;
  owned: boolean;
  points: number;
}

/** État d'un flag : validé ou non, et ce qu'il rapporte. */
export function FlagChip({ kind, owned, points }: FlagChipProps) {
  return (
    <span className={['flag-chip', owned && 'flag-chip--owned'].filter(Boolean).join(' ')}>
      <Icon name={owned ? 'check' : 'flag'} size={14} />
      <span>{FLAG_LABELS[kind]}</span>
      <span className="flag-chip__points">{points} pts</span>
    </span>
  );
}
