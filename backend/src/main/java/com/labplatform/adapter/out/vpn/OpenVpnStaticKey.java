package com.labplatform.adapter.out.vpn;

import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Clé partagée tls-crypt au format « OpenVPN Static key V1 » (2048 bits),
 * identique à ce que produit « openvpn --genkey secret ». Générée en Java pour
 * ne pas exiger le binaire openvpn sur le serveur de la plateforme.
 */
final class OpenVpnStaticKey {

    private static final int KEY_BYTES = 256;
    private static final int BYTES_PER_LINE = 16;

    private OpenVpnStaticKey() {
    }

    static String generate(SecureRandom random) {
        byte[] key = new byte[KEY_BYTES];
        random.nextBytes(key);
        HexFormat hex = HexFormat.of();
        StringBuilder out = new StringBuilder()
                .append("#\n# 2048 bit OpenVPN static key\n#\n")
                .append("-----BEGIN OpenVPN Static key V1-----\n");
        for (int offset = 0; offset < KEY_BYTES; offset += BYTES_PER_LINE) {
            out.append(hex.formatHex(key, offset, offset + BYTES_PER_LINE)).append('\n');
        }
        return out.append("-----END OpenVPN Static key V1-----\n").toString();
    }
}
