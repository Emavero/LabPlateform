package com.labplatform.application.port.in.vpn;

import com.labplatform.domain.user.Actor;

public interface RegenerateVpnProfileUseCase {

    /** Révoque le certificat actuel (l'ancien fichier cesse de fonctionner) et en émet un nouveau. */
    VpnAccess regenerate(Actor actor);
}
