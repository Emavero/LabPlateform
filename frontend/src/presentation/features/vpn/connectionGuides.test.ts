import { describe, expect, it } from 'vitest';
import { CONNECTION_GUIDES, detectClientOs } from './connectionGuides';

describe('guides de connexion VPN', () => {
  it('donne la commande OpenVPN pour Linux', () => {
    expect(CONNECTION_GUIDES.linux.command?.('cyberMans-lab-udp.ovpn')).toBe(
      'sudo openvpn --config cyberMans-lab-udp.ovpn',
    );
  });

  /** Les étapes sont des clés de traduction : c'est le catalogue qui les formule. */
  it('décrit chaque système par trois étapes', () => {
    expect(CONNECTION_GUIDES.windows.steps).toEqual([
      'vpn.guide.windows.1',
      'vpn.guide.windows.2',
      'vpn.guide.windows.3',
    ]);
    expect(CONNECTION_GUIDES.macos.steps).toHaveLength(3);
  });

  it('détecte le système du visiteur', () => {
    expect(detectClientOs('Mozilla/5.0 (Windows NT 10.0; Win64; x64)')).toBe('windows');
    expect(detectClientOs('Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5)')).toBe('macos');
    expect(detectClientOs('Mozilla/5.0 (X11; Linux x86_64)')).toBe('linux');
  });
});
