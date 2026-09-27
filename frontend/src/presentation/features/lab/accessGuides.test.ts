import { describe, expect, it } from 'vitest';
import { ACCESS_GUIDES } from './accessGuides';

describe('ACCESS_GUIDES', () => {
  it('construit la commande SSH avec utilisateur, hôte et port', () => {
    const command = ACCESS_GUIDES.SSH.command({ host: '10.42.0.12', port: 22, protocol: 'SSH', username: 'labuser', password: 'x' });
    expect(command).toBe('ssh labuser@10.42.0.12 -p 22');
  });

  it('construit la commande RDP avec hôte et port', () => {
    const command = ACCESS_GUIDES.RDP.command({ host: '10.42.0.11', port: 3389, protocol: 'RDP', username: 'labuser', password: 'x' });
    expect(command).toBe('mstsc /v:10.42.0.11:3389');
  });

  /** Les étapes sont des clés de traduction : c'est le catalogue qui les formule. */
  it('décrit chaque protocole par des clés de traduction', () => {
    expect(ACCESS_GUIDES.SSH.client).toBe('access.ssh.client');
    expect(ACCESS_GUIDES.SSH.steps).toEqual(['access.ssh.1', 'access.ssh.2', 'access.ssh.3', 'access.ssh.4']);
    expect(ACCESS_GUIDES.RDP.steps.every((key) => key.startsWith('access.rdp.'))).toBe(true);
  });
});
