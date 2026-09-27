package com.labplatform.domain.exposure;

import com.labplatform.domain.lab.OperatingSystem;

/**
 * Service d'accès qu'une cible présente au réseau du lab.
 * <p>
 * Il est déduit du système de la cible, et non découvert par un balayage de
 * ports : la plateforme sait ce qu'elle installe, et prétendre scanner
 * donnerait une fausse impression d'exhaustivité. Ce que ce module montre est
 * la surface <em>déclarée</em> du lab, celle que la plateforme ouvre
 * elle-même — pas le résultat d'une reconnaissance.
 */
public enum ExposedService {

    SSH("SSH", 22, "Accès distant en ligne de commande"),
    RDP("RDP", 3389, "Bureau à distance");

    private final String label;
    private final int port;
    private final String description;

    ExposedService(String label, int port, String description) {
        this.label = label;
        this.port = port;
        this.description = description;
    }

    /** Service ouvert par défaut sur ce système. */
    public static ExposedService of(OperatingSystem system) {
        return system == OperatingSystem.WINDOWS ? RDP : SSH;
    }

    public String label() {
        return label;
    }

    public int port() {
        return port;
    }

    public String description() {
        return description;
    }
}
