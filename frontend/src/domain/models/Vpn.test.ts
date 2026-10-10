import { describe, expect, it } from 'vitest';
import { defaultProtocol, isUploadedSource, type VpnAccess, needsProfileBeforeStarting } from './Vpn';

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

describe('avertissement avant de démarrer une machine', () => {
  it('avertit l’apprenant qui n’a jamais pris son profil', () => {
    expect(needsProfileBeforeStarting({ ...base, issuedAt: null })).toBe(true);
  });

  it('se taît dès que le profil a été émis', () => {
    expect(needsProfileBeforeStarting({ ...base, issuedAt: new Date('2026-10-10T09:00:00Z') })).toBe(false);
  });

  it('ne dit rien avec un profil déposé par l’administration', () => {
    // La date est celle du dépôt, pas celle de cet apprenant : avertir
    // accuserait à tort quelqu’un qui est déjà connecté.
    expect(needsProfileBeforeStarting({ ...base, source: 'uploaded', issuedAt: null })).toBe(false);
    expect(needsProfileBeforeStarting({ ...base, source: 'uploaded', issuedAt: new Date() })).toBe(false);
  });

  it('ne dit rien quand le VPN n’est pas configuré', () => {
    // Un autre écran l’annonce déjà : deux messages pour une même absence.
    expect(needsProfileBeforeStarting({ ...base, enabled: false, issuedAt: null })).toBe(false);
    expect(needsProfileBeforeStarting(null)).toBe(false);
    expect(needsProfileBeforeStarting(undefined)).toBe(false);
  });
});
