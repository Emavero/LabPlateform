package com.labplatform.application.port.out;

import com.labplatform.domain.vpn.VpnProfile;

import java.util.Optional;

public interface VpnProfileRepositoryPort {

    Optional<VpnProfile> findByUserId(Long userId);

    VpnProfile save(VpnProfile profile);
}
