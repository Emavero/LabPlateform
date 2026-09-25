package com.labplatform.adapter.in.web.dto;

import com.labplatform.application.port.in.vpn.VpnAccess;

import java.time.Instant;
import java.util.List;

public final class VpnDtos {

    private VpnDtos() {
    }

    public record VpnEndpointResponse(String protocol, String host, int port) {
    }

    /** issuedAt vaut null tant que l'utilisateur n'a jamais téléchargé son profil. */
    public record VpnAccessResponse(boolean enabled, Instant issuedAt, List<VpnEndpointResponse> endpoints,
                                    String labNetwork) {

        public static VpnAccessResponse from(VpnAccess access) {
            return new VpnAccessResponse(
                    access.enabled(),
                    access.issuedAt().orElse(null),
                    access.endpoints().stream()
                            .map(e -> new VpnEndpointResponse(e.protocol().parameter(), e.host(), e.port()))
                            .toList(),
                    access.labNetwork());
        }
    }
}
