interface AvatarProps {
  initials: string;
  size?: 'sm' | 'md' | 'lg';
}

export function Avatar({ initials, size = 'md' }: AvatarProps) {
  return (
    <span className={`avatar avatar--${size}`} aria-hidden="true">
      {initials}
    </span>
  );
}
