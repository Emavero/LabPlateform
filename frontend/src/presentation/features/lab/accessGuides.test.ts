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
});
