package com.labplatform.domain.vpn;

import com.labplatform.domain.shared.InvalidInputException;

import java.util.Objects;

/**
 * Réseau des machines du lab, écrit en notation CIDR.
 * <p>
 * OpenVPN ne lit pas le CIDR dans une directive {@code route} : il attend une
 * adresse et un masque. La conversion vit ici, dans le domaine, parce qu'elle
 * est une règle de calcul et non un détail de rendu — et parce qu'une erreur de
 * masque donne un profil qui se connecte sans rien joindre, panne silencieuse
 * s'il en est.
 */
public record LabNetwork(String address, String netmask) {

    public LabNetwork {
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(netmask, "netmask");
    }

    /**
     * Analyse une notation CIDR (« 10.10.10.0/24 »).
     * <p>
     * L'adresse est ramenée à celle du réseau : « 10.10.10.7/24 » désigne le
     * même réseau que « 10.10.10.0/24 », et OpenVPN refuse une route dont
     * l'adresse porte des bits hors du masque.
     */
    public static LabNetwork ofCidr(String cidr) {
        String text = cidr == null ? "" : cidr.trim();
        int slash = text.indexOf('/');
        if (slash < 0) {
            throw new InvalidInputException("Réseau du lab invalide : « " + cidr + " » (attendu : 10.10.10.0/24)");
        }
        int prefix = prefixOf(text.substring(slash + 1), cidr);
        long bits = addressOf(text.substring(0, slash), cidr);
        // Masque de n bits de poids fort ; 0 donnerait un décalage de 32, que
        // Java traite comme un décalage de 0 — d'où le cas séparé.
        long mask = prefix == 0 ? 0L : (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
        return new LabNetwork(toDotted(bits & mask), toDotted(mask));
    }

    private static int prefixOf(String text, String cidr) {
        try {
            int prefix = Integer.parseInt(text);
            if (prefix < 0 || prefix > 32) {
                throw new NumberFormatException(text);
            }
            return prefix;
        } catch (NumberFormatException e) {
            throw new InvalidInputException("Préfixe de réseau invalide dans « " + cidr + " »");
        }
    }

    private static long addressOf(String text, String cidr) {
        String[] parts = text.split("\\.");
        if (parts.length != 4) {
            throw new InvalidInputException("Adresse de réseau invalide dans « " + cidr + " »");
        }
        long value = 0;
        for (String part : parts) {
            int octet;
            try {
                octet = Integer.parseInt(part);
            } catch (NumberFormatException e) {
                throw new InvalidInputException("Adresse de réseau invalide dans « " + cidr + " »");
            }
            if (octet < 0 || octet > 255) {
                throw new InvalidInputException("Adresse de réseau invalide dans « " + cidr + " »");
            }
            value = (value << 8) | octet;
        }
        return value;
    }

    private static String toDotted(long value) {
        return ((value >> 24) & 0xFF) + "." + ((value >> 16) & 0xFF) + "."
                + ((value >> 8) & 0xFF) + "." + (value & 0xFF);
    }
}
