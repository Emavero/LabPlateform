package com.labplatform.domain.vpn;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Accès VPN d'un utilisateur : le nom du certificat client qui lui est
 * attribué. Un seul profil actif par utilisateur ; le régénérer révoque
 * l'ancien certificat et en émet un nouveau.
 */
public final class VpnProfile {

    /** Le nom devient un nom de fichier et un argument de commande : format strict. */
    private static final Pattern COMMON_NAME = Pattern.compile("cyberMans-u\\d{1,19}-[a-z0-9]{6,16}");

    private final Long userId;
    private final String commonName;
    private final Instant issuedAt;

    private VpnProfile(Long userId, String commonName, Instant issuedAt) {
        this.userId = Objects.requireNonNull(userId, "userId");
        this.commonName = Objects.requireNonNull(commonName, "commonName");
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        if (!COMMON_NAME.matcher(commonName).matches()) {
            throw new IllegalArgumentException("Nom de certificat invalide : " + commonName);
        }
    }

    public static String commonNameFor(Long userId, String randomSuffix) {
        return "cyberMans-u" + userId + "-" + randomSuffix;
    }

    public static VpnProfile issued(Long userId, String commonName, Instant now) {
        return new VpnProfile(userId, commonName, now);
    }

    public static VpnProfile restore(Long userId, String commonName, Instant issuedAt) {
        return new VpnProfile(userId, commonName, issuedAt);
    }

    public static boolean isValidCommonName(String commonName) {
        return commonName != null && COMMON_NAME.matcher(commonName).matches();
    }

    public Long getUserId() {
        return userId;
    }

    public String getCommonName() {
        return commonName;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }
}
