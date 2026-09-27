package com.labplatform.domain.exposure;

/**
 * Niveau d'exposition d'une cible, en quatre paliers.
 * <p>
 * Quatre, et non un score brut affiché tel quel : un « 63 sur 100 » invite à
 * comparer des chiffres qui n'ont pas cette précision. Le score sert à classer,
 * le palier à décider.
 */
public enum ExposureLevel {

    CRITICAL("Critique"),
    HIGH("Élevée"),
    MODERATE("Modérée"),
    LOW("Faible");

    private final String displayName;

    ExposureLevel(String displayName) {
        this.displayName = displayName;
    }

    public static ExposureLevel of(int score) {
        if (score >= 75) {
            return CRITICAL;
        }
        if (score >= 50) {
            return HIGH;
        }
        if (score >= 25) {
            return MODERATE;
        }
        return LOW;
    }

    public String displayName() {
        return displayName;
    }
}
