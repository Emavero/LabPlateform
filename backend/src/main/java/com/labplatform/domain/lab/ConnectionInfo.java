package com.labplatform.domain.lab;

import java.util.Objects;

/**
 * Informations d'accès d'une machine démarrée. N'existe que pendant
 * qu'une VM tourne : l'arrêt les efface (voir VirtualMachine.stop()).
 */
public record ConnectionInfo(String host, int port, AccessProtocol protocol, String username, String password) {

    public ConnectionInfo {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(protocol, "protocol");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(password, "password");
        if (host.isBlank()) {
            throw new IllegalArgumentException("host ne peut pas être vide");
        }
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("port hors limites : " + port);
        }
    }

    @Override
    public String toString() {
        // Ne jamais laisser fuiter le mot de passe dans les journaux.
        return "ConnectionInfo[" + protocol + "://" + username + "@" + host + ":" + port + "]";
    }
}
