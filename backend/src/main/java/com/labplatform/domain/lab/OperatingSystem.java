package com.labplatform.domain.lab;

/**
 * Systèmes proposés dans un lab. Chaque OS fixe son libellé et son
 * protocole d'accès : ajouter un OS revient à ajouter une constante ici.
 */
public enum OperatingSystem {
    WINDOWS("Windows Server 2022", AccessProtocol.RDP),
    LINUX("Ubuntu 24.04 LTS", AccessProtocol.SSH);

    private final String displayName;
    private final AccessProtocol protocol;

    OperatingSystem(String displayName, AccessProtocol protocol) {
        this.displayName = displayName;
        this.protocol = protocol;
    }

    public String displayName() {
        return displayName;
    }

    public AccessProtocol protocol() {
        return protocol;
    }
}
