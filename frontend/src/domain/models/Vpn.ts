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
  /**
   * Date d'émission du profil actuel ; null tant qu'il n'a jamais été
   * téléchargé. En source déposée, c'est la date du dépôt par l'administration.
   */
  readonly issuedAt: Date | null;
  readonly endpoints: readonly VpnEndpoint[];
  /** Réseau joignable une fois connecté, ex. 10.10.10.0/24. */
  readonly labNetwork: string;
  readonly source: VpnSource;
}

/**
 * D'où vient le profil.
 * <p>
 * « generated » : un certificat par personne, révocable, avec un protocole à
 * choisir. « uploaded » : un fichier déposé par l'administration, le même pour
 * tous — ni protocole à choisir, ni régénération possible.
 */
export type VpnSource = 'generated' | 'uploaded';

/** En source déposée, il n'y a qu'un fichier : rien à choisir avant de le prendre. */
export function isUploadedSource(access: VpnAccess): boolean {
  return access.source === 'uploaded';
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

/**
 * Profil déposé par l'administration, tel qu'elle le voit.
 * <p>
 * Jamais son contenu : il porte une clé privée, et l'écran n'en a pas besoin.
 */
export interface LabVpnProfile {
  readonly present: boolean;
  readonly fileName: string | null;
  readonly sizeBytes: number;
  readonly uploadedAt: Date | null;
  /**
   * Faux quand le profil ne route pas le réseau des machines : le tunnel
   * montera sans les joindre. Un avertissement, pas un refus — le serveur
   * OpenVPN peut pousser la route lui-même.
   */
  readonly routesLabNetwork: boolean;
}

/** Taille lisible d'un profil : « 7,4 Ko » plutôt que « 7589 ». */
export function formatProfileSize(bytes: number): string {
  return bytes < 1024 ? `${bytes} o` : `${(bytes / 1024).toFixed(1)} Ko`;
}
