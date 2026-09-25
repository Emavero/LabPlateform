package com.labplatform.application.port.in.vpn;

import com.labplatform.domain.user.Actor;
import com.labplatform.domain.vpn.VpnProtocol;

public interface DownloadVpnProfileUseCase {

    /** Renvoie le profil personnel de l'utilisateur, en l'émettant au premier téléchargement. */
    VpnProfileFile download(Actor actor, VpnProtocol protocol);
}
