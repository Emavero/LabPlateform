import { describe, expect, it } from 'vitest';
import { defaultProtocol, type VpnAccess } from './Vpn';

const base: VpnAccess = { enabled: true, issuedAt: null, endpoints: [], labNetwork: '10.10.10.0/24' };

describe('defaultProtocol', () => {
  it('privilégie UDP quand il est proposé', () => {
    const access = {
      ...base,
      endpoints: [
        { protocol: 'tcp', host: 'vpn', port: 443 },
        { protocol: 'udp', host: 'vpn', port: 1194 },
      ],
    } satisfies VpnAccess;
    expect(defaultProtocol(access)).toBe('udp');
  });

  it('se rabat sur TCP s’il est le seul proposé (cas ngrok)', () => {
    expect(defaultProtocol({ ...base, endpoints: [{ protocol: 'tcp', host: '5.tcp.eu.ngrok.io', port: 12345 }] })).toBe('tcp');
  });

  it('ne propose rien sans point d’entrée', () => {
    expect(defaultProtocol(base)).toBeNull();
  });
});
