import type { SVGProps } from 'react';

/**
 * Jeu d'icônes en SVG inline (trait 1.75, 24×24) : aucune dépendance externe,
 * couleur héritée via currentColor. Ajouter une icône = ajouter une entrée
 * dans PATHS, sans toucher au composant (OCP).
 */
const PATHS = {
  shield: (
    <>
      <path d="M12 3 4.5 6v5.5c0 4.6 3.1 8.3 7.5 9.5 4.4-1.2 7.5-4.9 7.5-9.5V6L12 3Z" />
      <path d="m9 12 2 2 4-4" />
    </>
  ),
  dashboard: (
    <>
      <rect x="3.5" y="3.5" width="7" height="8" rx="1.5" />
      <rect x="13.5" y="3.5" width="7" height="5" rx="1.5" />
      <rect x="13.5" y="11.5" width="7" height="9" rx="1.5" />
      <rect x="3.5" y="14.5" width="7" height="6" rx="1.5" />
    </>
  ),
  server: (
    <>
      <rect x="3.5" y="4" width="17" height="7" rx="1.5" />
      <rect x="3.5" y="13" width="17" height="7" rx="1.5" />
      <path d="M7 7.5h.01M7 16.5h.01M11 7.5h6M11 16.5h6" />
    </>
  ),
  radar: (
    <>
      <circle cx="12" cy="12" r="8.5" />
      <circle cx="12" cy="12" r="4.5" />
      <path d="M12 12 18 6" />
      <circle cx="12" cy="12" r="0.8" fill="currentColor" />
    </>
  ),
  route: (
    <>
      <circle cx="6" cy="18" r="2.2" />
      <circle cx="18" cy="6" r="2.2" />
      <path d="M8.2 18H15a3 3 0 0 0 0-6H9a3 3 0 0 1 0-6h6.8" />
    </>
  ),
  activity: <path d="M3 12h4l2.5-6 5 12 2.5-6h4" />,
  scenario: (
    <>
      <rect x="3.5" y="3.5" width="6" height="6" rx="1.5" />
      <rect x="14.5" y="14.5" width="6" height="6" rx="1.5" />
      <path d="M9.5 6.5h5a3 3 0 0 1 3 3v5M6.5 9.5v5a3 3 0 0 0 3 3h5" />
    </>
  ),
  admin: (
    <>
      <circle cx="9" cy="8" r="3.5" />
      <path d="M2.5 20a6.5 6.5 0 0 1 11.3-4.4" />
      <path d="M18 14.5v1.2M18 20.3v1.2M15 18h-1.2M22.2 18H21M15.9 15.9l-.8-.8M20.9 20.9l-.8-.8M15.9 20.1l-.8.8M20.9 15.1l-.8.8" />
      <circle cx="18" cy="18" r="2" />
    </>
  ),
  report: (
    <>
      <path d="M14 3.5H7a1.5 1.5 0 0 0-1.5 1.5v14A1.5 1.5 0 0 0 7 20.5h10a1.5 1.5 0 0 0 1.5-1.5V8L14 3.5Z" />
      <path d="M14 3.5V8h4.5M9 13h6M9 16.5h4" />
    </>
  ),
  settings: (
    <>
      <path d="M4 6.5h9M17 6.5h3M4 12h3M11 12h9M4 17.5h11M19 17.5h1" />
      <circle cx="15" cy="6.5" r="2" />
      <circle cx="9" cy="12" r="2" />
      <circle cx="17" cy="17.5" r="2" />
    </>
  ),
  support: (
    <>
      <circle cx="12" cy="12" r="8.5" />
      <circle cx="12" cy="12" r="3.5" />
      <path d="m6 6 3.5 3.5M14.5 14.5 18 18M18 6l-3.5 3.5M9.5 14.5 6 18" />
    </>
  ),
  chevronsUpDown: <path d="m8 9 4-4 4 4M8 15l4 4 4-4" />,
  chevronRight: <path d="m9 6 6 6-6 6" />,
  logout: (
    <>
      <path d="M9.5 20.5H6A1.5 1.5 0 0 1 4.5 19V5A1.5 1.5 0 0 1 6 3.5h3.5" />
      <path d="M15 16.5 19.5 12 15 7.5M19.5 12H9.5" />
    </>
  ),
  user: (
    <>
      <circle cx="12" cy="8" r="4" />
      <path d="M4.5 20.5a7.5 7.5 0 0 1 15 0" />
    </>
  ),
  copy: (
    <>
      <rect x="8.5" y="8.5" width="12" height="12" rx="2" />
      <path d="M15.5 8.5V5A1.5 1.5 0 0 0 14 3.5H5A1.5 1.5 0 0 0 3.5 5v9A1.5 1.5 0 0 0 5 15.5h3.5" />
    </>
  ),
  check: <path d="m5 12.5 4.5 4.5L19 7.5" />,
  eye: (
    <>
      <path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12Z" />
      <circle cx="12" cy="12" r="3" />
    </>
  ),
  eyeOff: (
    <>
      <path d="M10.6 5.6A9.8 9.8 0 0 1 12 5.5c6 0 9.5 6.5 9.5 6.5a17 17 0 0 1-2.7 3.4M6.3 6.8C3.9 8.5 2.5 12 2.5 12S6 18.5 12 18.5a9.3 9.3 0 0 0 4.7-1.3" />
      <path d="M9.9 9.9a3 3 0 0 0 4.2 4.2M3.5 3.5l17 17" />
    </>
  ),
  play: <path d="M8 5.5v13l10.5-6.5L8 5.5Z" />,
  stop: <rect x="6.5" y="6.5" width="11" height="11" rx="2" />,
  terminal: (
    <>
      <rect x="3" y="4.5" width="18" height="15" rx="2" />
      <path d="m7 9.5 3 2.5-3 2.5M12.5 15h4.5" />
    </>
  ),
  windows: (
    <>
      <path d="M3.5 5.8 10.5 4.8v6.7h-7V5.8ZM12.5 4.5l8-1v8h-8v-7ZM3.5 13.5h7v6.7l-7-1v-5.7ZM12.5 13.5h8v8l-8-1.1v-6.9Z" />
    </>
  ),
  linux: (
    <>
      <path d="M12 3c-2.2 0-3.4 1.9-3.4 4.2 0 1.5.3 2.3-.8 4-1.3 2-2.8 3.9-2.8 6.1 0 .8.3 1.4.8 1.9M12 3c2.2 0 3.4 1.9 3.4 4.2 0 1.5-.3 2.3.8 4 1.3 2 2.8 3.9 2.8 6.1 0 .8-.3 1.4-.8 1.9" />
      <path d="M8.5 19.5c1 1 2.2 1.5 3.5 1.5s2.5-.5 3.5-1.5M10.3 7.6h.01M13.7 7.6h.01M10.5 10c.5.5 1 .7 1.5.7s1-.2 1.5-.7" />
    </>
  ),
  arrowLeft: <path d="M19.5 12h-15M10.5 6 4.5 12l6 6" />,
  menu: <path d="M4 7h16M4 12h16M4 17h16" />,
  x: <path d="M6 6l12 12M18 6 6 18" />,
  alert: (
    <>
      <path d="M12 4 2.8 19.5h18.4L12 4Z" />
      <path d="M12 10v4M12 17h.01" />
    </>
  ),
  info: (
    <>
      <circle cx="12" cy="12" r="8.5" />
      <path d="M12 11v5M12 8h.01" />
    </>
  ),
  refresh: (
    <>
      <path d="M20 11a8 8 0 0 0-14.3-4.3L4 8.5" />
      <path d="M4 4v4.5h4.5M4 13a8 8 0 0 0 14.3 4.3L20 15.5" />
      <path d="M20 20v-4.5h-4.5" />
    </>
  ),
  key: (
    <>
      <circle cx="8" cy="15" r="4" />
      <path d="m11 12 8.5-8.5M16.5 6.5l2.5 2.5M14 9l2 2" />
    </>
  ),
  lock: (
    <>
      <rect x="4.5" y="10.5" width="15" height="10" rx="2" />
      <path d="M8 10.5V7.5a4 4 0 0 1 8 0v3" />
    </>
  ),
  download: (
    <>
      <path d="M12 3.5v11M7.5 10 12 14.5 16.5 10" />
      <path d="M4.5 16v2.5A1.5 1.5 0 0 0 6 20h12a1.5 1.5 0 0 0 1.5-1.5V16" />
    </>
  ),
  vpn: (
    <>
      <path d="M12 3 4.5 6v5.5c0 4.6 3.1 8.3 7.5 9.5 4.4-1.2 7.5-4.9 7.5-9.5V6L12 3Z" />
      <rect x="9" y="11" width="6" height="5" rx="1" />
      <path d="M10.25 11V9.5a1.75 1.75 0 0 1 3.5 0V11" />
    </>
  ),
  mail: (
    <>
      <rect x="3" y="5" width="18" height="14" rx="2" />
      <path d="m3.5 6.5 8.5 6.5 8.5-6.5" />
    </>
  ),
  flag: (
    <>
      <path d="M6 21V4.5" />
      <path d="M6 5h9.5l-1.5 3.5L15.5 12H6" />
    </>
  ),
  trophy: (
    <>
      <path d="M8 4h8v5a4 4 0 0 1-8 0V4Z" />
      <path d="M8 5.5H5.5v1.5a3 3 0 0 0 3 3M16 5.5h2.5V7a3 3 0 0 1-3 3" />
      <path d="M12 13v3.5M9 20.5h6M9.5 20.5c0-2 1-4 2.5-4s2.5 2 2.5 4" />
    </>
  ),
  target: (
    <>
      <circle cx="12" cy="12" r="8.5" />
      <circle cx="12" cy="12" r="4" />
      <circle cx="12" cy="12" r="0.9" fill="currentColor" />
      <path d="M12 1.5v3M12 19.5v3M1.5 12h3M19.5 12h3" />
    </>
  ),
  crown: (
    <>
      <path d="M4 17.5h16M4.5 7l3.5 3.5L12 5l4 5.5L19.5 7l-1.5 8.5H6L4.5 7Z" />
    </>
  ),
} as const;

export type IconName = keyof typeof PATHS;

interface IconProps extends Omit<SVGProps<SVGSVGElement>, 'name'> {
  name: IconName;
  size?: number;
  /** Libellé accessible ; sans libellé, l'icône est décorative et masquée aux lecteurs d'écran. */
  label?: string;
}

export function Icon({ name, size = 20, label, ...rest }: IconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      role={label ? 'img' : undefined}
      aria-label={label}
      aria-hidden={label ? undefined : true}
      focusable="false"
      {...rest}
    >
      {PATHS[name]}
    </svg>
  );
}
