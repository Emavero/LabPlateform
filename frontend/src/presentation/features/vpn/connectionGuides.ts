export type ClientOs = 'linux' | 'windows' | 'macos';

export interface ConnectionGuide {
  readonly label: string;
  /** Commande à copier, si le système se pilote en ligne de commande. */
  readonly command?: (fileName: string) => string;
  readonly steps: (fileName: string) => readonly string[];
}

/** Un guide par système : en ajouter un ne modifie pas la page (OCP). */
export const CONNECTION_GUIDES: Readonly<Record<ClientOs, ConnectionGuide>> = {
  linux: {
    label: 'Linux',
    command: (file) => `sudo openvpn --config ${file}`,
    steps: () => [
      'Installez OpenVPN si besoin : sudo apt install openvpn (déjà présent sur Kali et Parrot).',
      'Ouvrez un terminal dans le dossier du fichier téléchargé et lancez la commande ci-dessus.',
      'Attendez le message « Initialization Sequence Completed », puis laissez le terminal ouvert pendant la session.',
    ],
  },
  windows: {
    label: 'Windows',
    steps: (file) => [
      'Installez OpenVPN Connect depuis openvpn.net.',
      `Ouvrez-le, choisissez « Import Profile » puis « Upload File », et sélectionnez ${file}.`,
      'Cliquez sur « Connect » : le statut passe à « Connected ».',
    ],
  },
  macos: {
    label: 'macOS',
    steps: (file) => [
      'Installez OpenVPN Connect depuis openvpn.net (ou Tunnelblick).',
      `Importez ${file} (glisser-déposer dans la fenêtre ou « Import Profile »).`,
      'Cliquez sur « Connect » et autorisez la configuration VPN si macOS le demande.',
    ],
  },
};

export function detectClientOs(userAgent: string = navigator.userAgent): ClientOs {
  if (/Windows/i.test(userAgent)) return 'windows';
  if (/Mac OS X|Macintosh/i.test(userAgent)) return 'macos';
  return 'linux';
}
