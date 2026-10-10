import { describe, expect, it } from 'vitest';
import { defaultProtocol, isUploadedSource, type VpnAccess } from './Vpn';

const base: VpnAccess = {
  enabled: true,
  issuedAt: null,
  endpoints: [],
  labNetwork: '10.10.10.0/24',
  source: 'generated',
};

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

describe('source du profil', () => {
  it('reconnaît un profil déposé par l’administration', () => {
    expect(isUploadedSource(base)).toBe(false);
    expect(isUploadedSource({ ...base, source: 'uploaded' })).toBe(true);
  });

  it('un profil déposé n’a pas de point d’entrée à choisir', () => {
    // Le fichier porte son serveur et son transport : rien à sélectionner.
    const uploaded: VpnAccess = { ...base, source: 'uploaded' };

    expect(uploaded.endpoints).toHaveLength(0);
    expect(defaultProtocol(uploaded)).toBeNull();
  });
});
