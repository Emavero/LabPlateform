export type VpnProtocol = 'udp' | 'tcp';

/** Point d'entrée public du serveur VPN pour un protocole. */
export interface VpnEndpoint {
  readonly protocol: VpnProtocol;
  readonly host: string;
  readonly port: number;
}

/** Accès VPN de l'utilisateur, façon Hack The Box : un profil .ovpn personnel. */
export interface VpnAccess {
  readonly enabled: boolean;
  /** Date d'émission du profil actuel ; null tant qu'il n'a jamais été téléchargé. */
  readonly issuedAt: Date | null;
  readonly endpoints: readonly VpnEndpoint[];
  /** Réseau joignable une fois connecté, ex. 10.10.10.0/24. */
  readonly labNetwork: string;
}

export interface VpnProfileDownload {
  readonly fileName: string;
  readonly content: string;
}

export const VPN_PROTOCOL_LABELS: Record<VpnProtocol, string> = {
  udp: 'UDP',
  tcp: 'TCP',
};

/** UDP est recommandé ; TCP reste le choix par défaut s'il est le seul proposé. */
export function defaultProtocol(access: VpnAccess): VpnProtocol | null {
  if (access.endpoints.some((e) => e.protocol === 'udp')) return 'udp';
  return access.endpoints[0]?.protocol ?? null;
}
