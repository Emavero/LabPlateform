import type { AccessProtocol, ConnectionInfo } from '@/domain/models/VirtualMachine';

export interface AccessGuide {
  /** Nom du client à utiliser. */
  readonly client: string;
  /** Commande prête à coller dans un terminal. */
  readonly command: (connection: ConnectionInfo) => string;
  readonly steps: (connection: ConnectionInfo) => readonly string[];
}

/**
 * Stratégie par protocole : un nouveau protocole (VNC, console web...)
 * s'ajoute ici sans modifier les composants qui affichent le guide (OCP).
 */
export const ACCESS_GUIDES: Readonly<Record<AccessProtocol, AccessGuide>> = {
  SSH: {
    client: 'Terminal (OpenSSH)',
    command: (c) => `ssh ${c.username}@${c.host} -p ${c.port}`,
    steps: (c) => [
      'Ouvrez un terminal (PowerShell, macOS Terminal ou Linux).',
      'Collez la commande ci-dessus et validez.',
      "Acceptez l'empreinte du serveur lors de la première connexion.",
      `Saisissez le mot de passe temporaire de ${c.username}.`,
    ],
  },
  RDP: {
    client: 'Bureau à distance (RDP)',
    command: (c) => `mstsc /v:${c.host}:${c.port}`,
    steps: (c) => [
      'Sous Windows, exécutez la commande ci-dessus (Win + R). Sur macOS ou Linux, utilisez Windows App ou Remmina.',
      `Renseignez l'hôte ${c.host}:${c.port}.`,
      `Connectez-vous avec l'utilisateur ${c.username} et le mot de passe temporaire.`,
      'Acceptez le certificat auto-signé de la machine de lab.',
    ],
  },
};
