package com.labplatform.application.port.in.vpn;

import com.labplatform.domain.user.Actor;

public interface GetVpnAccessUseCase {

    VpnAccess getAccess(Actor actor);
}
