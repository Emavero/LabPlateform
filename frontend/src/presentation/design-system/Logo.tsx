import { Icon } from './Icon';

export function Logo({ compact = false }: { compact?: boolean }) {
  return (
    <span className="logo">
      <span className="logo__mark">
        <Icon name="shield" size={22} />
      </span>
      {!compact && <span className="logo__text">cyberMans</span>}
    </span>
  );
}
