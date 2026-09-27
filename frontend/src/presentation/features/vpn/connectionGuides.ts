import type { CatalogueKey } from '../../i18n/I18nContext';

export type ClientOs = 'linux' | 'windows' | 'macos';

export interface ConnectionGuide {
  /** Nom du système, le même dans toutes les langues. */
  readonly label: string;
  /** Commande à copier, si le système se pilote en ligne de commande. */
  readonly command?: (fileName: string) => string;
  /**
   * Étapes, données par leur clé de traduction plutôt qu'en toutes lettres :
   * le guide dit quoi faire, le catalogue dit comment le formuler, et une
   * langue de plus n'oblige pas à toucher ce fichier.
   */
  readonly steps: readonly CatalogueKey[];
}

/** Un guide par système : en ajouter un ne modifie pas la page (OCP). */
export const CONNECTION_GUIDES: Readonly<Record<ClientOs, ConnectionGuide>> = {
  linux: {
    label: 'Linux',
    command: (file) => `sudo openvpn --config ${file}`,
    steps: ['vpn.guide.linux.1', 'vpn.guide.linux.2', 'vpn.guide.linux.3'],
  },
  windows: {
    label: 'Windows',
    steps: ['vpn.guide.windows.1', 'vpn.guide.windows.2', 'vpn.guide.windows.3'],
  },
  macos: {
    label: 'macOS',
    steps: ['vpn.guide.macos.1', 'vpn.guide.macos.2', 'vpn.guide.macos.3'],
  },
};

export function detectClientOs(userAgent: string = navigator.userAgent): ClientOs {
  if (/Windows/i.test(userAgent)) return 'windows';
  if (/Mac OS X|Macintosh/i.test(userAgent)) return 'macos';
  return 'linux';
}
