import type { AccessProtocol, ConnectionInfo } from '@/domain/models/VirtualMachine';
import type { CatalogueKey } from '../../i18n/I18nContext';

export interface AccessGuide {
  /** Nom du client à utiliser, donné par sa clé de traduction. */
  readonly client: CatalogueKey;
  /** Commande prête à coller dans un terminal. */
  readonly command: (connection: ConnectionInfo) => string;
  /**
   * Étapes, données par leur clé de traduction plutôt qu'en toutes lettres :
   * le guide dit quoi faire, le catalogue dit comment le formuler, et une
   * langue de plus n'oblige pas à toucher ce fichier. Les clés reçoivent
   * l'utilisateur, l'hôte et le port de la machine lancée.
   */
  readonly steps: readonly CatalogueKey[];
}

/**
 * Stratégie par protocole : un nouveau protocole (VNC, console web...)
 * s'ajoute ici sans modifier les composants qui affichent le guide (OCP).
 */
export const ACCESS_GUIDES: Readonly<Record<AccessProtocol, AccessGuide>> = {
  SSH: {
    client: 'access.ssh.client',
    command: (c) => `ssh ${c.username}@${c.host} -p ${c.port}`,
    steps: ['access.ssh.1', 'access.ssh.2', 'access.ssh.3', 'access.ssh.4'],
  },
  RDP: {
    client: 'access.rdp.client',
    command: (c) => `mstsc /v:${c.host}:${c.port}`,
    steps: ['access.rdp.1', 'access.rdp.2', 'access.rdp.3', 'access.rdp.4'],
  },
};
