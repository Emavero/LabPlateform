package com.labplatform.application.port.in.vpn;

/** Fichier .ovpn prêt à être téléchargé. Contient la clé privée : ne jamais le journaliser. */
public record VpnProfileFile(String fileName, String content) {

    @Override
    public String toString() {
        return "VpnProfileFile[" + fileName + "]";
    }
}
