package com.labplatform.domain.box;

import com.labplatform.domain.shared.InvalidInputException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Flag d'une machine, tel qu'il est conservé : uniquement son empreinte
 * SHA-256, jamais sa valeur. Une lecture de la base ne permet donc pas de
 * valider les machines sans les avoir compromises.
 * <p>
 * Un flag est une chaîne de 32 caractères hexadécimaux, comme sur la
 * plupart des plateformes d'entraînement. La comparaison est faite en temps
 * constant : le temps de réponse ne révèle pas le nombre de caractères
 * corrects d'une soumission.
 */
public record Flag(String hash) {

    private static final Pattern SECRET_FORMAT = Pattern.compile("^[0-9a-f]{32}$");
    private static final Pattern HASH_FORMAT = Pattern.compile("^[0-9a-f]{64}$");

    public Flag {
        Objects.requireNonNull(hash, "hash");
        if (!HASH_FORMAT.matcher(hash).matches()) {
            throw new IllegalArgumentException("Empreinte de flag invalide");
        }
    }

    /** Depuis la valeur en clair, qui n'est pas conservée. */
    public static Flag ofSecret(String secret) {
        return new Flag(hashOf(requireWellFormed(secret)));
    }

    /** Depuis la persistance. */
    public static Flag ofHash(String hash) {
        return new Flag(hash);
    }

    /**
     * Vrai si la soumission correspond à ce flag. Une soumission mal formée
     * est refusée avant toute comparaison : inutile de hacher du bruit.
     */
    public boolean matches(String submitted) {
        byte[] expected = hash.getBytes(StandardCharsets.UTF_8);
        byte[] actual = hashOf(requireWellFormed(submitted)).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    /** Normalise une saisie (espaces, casse, accolades du type « CYBM{…} ») et en vérifie le format. */
    public static String requireWellFormed(String submitted) {
        if (submitted == null || submitted.isBlank()) {
            throw new InvalidInputException("Le flag est obligatoire");
        }
        String normalized = submitted.trim().toLowerCase(Locale.ROOT);
        int opening = normalized.indexOf('{');
        int closing = normalized.lastIndexOf('}');
        if (opening >= 0 && closing > opening) {
            normalized = normalized.substring(opening + 1, closing).trim();
        }
        if (!SECRET_FORMAT.matcher(normalized).matches()) {
            throw new InvalidInputException("Un flag est une suite de 32 caractères hexadécimaux");
        }
        return normalized;
    }

    private static String hashOf(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(secret.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    @Override
    public String toString() {
        // Ne jamais laisser fuiter l'empreinte dans les journaux.
        return "Flag[****]";
    }
}
