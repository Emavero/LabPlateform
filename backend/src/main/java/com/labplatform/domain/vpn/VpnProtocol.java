package com.labplatform.domain.vpn;

import com.labplatform.domain.shared.InvalidInputException;

import java.util.Locale;

/** Transport du tunnel : UDP (plus rapide) ou TCP (passe les réseaux qui filtrent l'UDP). */
public enum VpnProtocol {
    UDP,
    TCP;

    public static VpnProtocol fromParameter(String value) {
        if (value != null) {
            for (VpnProtocol protocol : values()) {
                if (protocol.name().equalsIgnoreCase(value.trim())) {
                    return protocol;
                }
            }
        }
        throw new InvalidInputException("Protocole VPN inconnu : utilisez udp ou tcp");
    }

    public String parameter() {
        return name().toLowerCase(Locale.ROOT);
    }
}
