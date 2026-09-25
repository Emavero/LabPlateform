import type { VpnAccess, VpnProfileDownload, VpnProtocol } from '../models/Vpn';
import type { VpnRepository } from '../repositories/VpnRepository';

export class GetVpnAccessUseCase {
  constructor(private readonly vpn: VpnRepository) {}

  execute(): Promise<VpnAccess> {
    return this.vpn.getAccess();
  }
}

/** Récupère le profil personnel ; le serveur l'émet au premier téléchargement. */
export class DownloadVpnProfileUseCase {
  constructor(private readonly vpn: VpnRepository) {}

  execute(protocol: VpnProtocol): Promise<VpnProfileDownload> {
    return this.vpn.downloadProfile(protocol);
  }
}

/** Révoque le profil actuel (l'ancien fichier cesse de fonctionner) et en émet un nouveau. */
export class RegenerateVpnProfileUseCase {
  constructor(private readonly vpn: VpnRepository) {}

  execute(): Promise<VpnAccess> {
    return this.vpn.regenerate();
  }
}
