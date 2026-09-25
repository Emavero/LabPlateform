package com.labplatform.domain.lab;

/** Protocole d'accès distant exposé par une machine, avec son port standard. */
public enum AccessProtocol {
    RDP(3389),
    SSH(22);

    private final int defaultPort;

    AccessProtocol(int defaultPort) {
        this.defaultPort = defaultPort;
    }

    public int defaultPort() {
        return defaultPort;
    }
}
