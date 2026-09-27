import type { FlagKind } from '@/domain/models/Box';
import { Icon } from '../../design-system';
import { useI18n } from '../../i18n/I18nContext';

interface FlagChipProps {
  kind: FlagKind;
  owned: boolean;
  points: number;
}

/** État d'un flag : validé ou non, et ce qu'il rapporte. */
export function FlagChip({ kind, owned, points }: FlagChipProps) {
  const { t } = useI18n();
  return (
    <span className={['flag-chip', owned && 'flag-chip--owned'].filter(Boolean).join(' ')}>
      <Icon name={owned ? 'check' : 'flag'} size={14} />
      <span>{t(`flag.${kind}`)}</span>
      <span className="flag-chip__points">{points} pts</span>
    </span>
  );
}
