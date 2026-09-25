package com.labplatform.domain.vpn;

import java.util.Objects;

/** Point d'entrée public du serveur VPN pour un protocole donné. */
public record VpnEndpoint(VpnProtocol protocol, String host, int port) {

    public VpnEndpoint {
        Objects.requireNonNull(protocol, "protocol");
        Objects.requireNonNull(host, "host");
        if (host.isBlank() || !host.matches("[A-Za-z0-9.:\\-\\[\\]]+")) {
            throw new IllegalArgumentException("Hôte VPN invalide : " + host);
        }
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("Port VPN hors limites : " + port);
        }
    }
}
