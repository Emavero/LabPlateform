package com.labplatform.domain.media;

import com.labplatform.domain.shared.InvalidInputException;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Fichier téléversé sur la plateforme, aujourd'hui une vidéo de cours.
 * <p>
 * Deux protections tiennent l'essentiel. Le type est choisi dans une liste
 * fermée : un fichier téléversé est renvoyé plus tard à des navigateurs, et
 * laisser passer du HTML reviendrait à héberger du script sur notre propre
 * origine. Et l'identifiant est tiré au hasard, jamais dérivé du nom du
 * fichier : rien de ce que l'auteur écrit ne se retrouve dans un chemin.
 */
public record MediaAsset(String id, String filename, String contentType, long sizeBytes, Instant uploadedAt,
                         Long uploadedBy) {

    /** Formats lisibles nativement par les navigateurs, et eux seuls. */
    public static final Set<String> SUPPORTED_TYPES = Set.of("video/mp4", "video/webm", "video/ogg");

    private static final Pattern ID_FORMAT = Pattern.compile("^[0-9a-f]{32}$");
    private static final int MAX_FILENAME_LENGTH = 128;

    public MediaAsset {
        Objects.requireNonNull(id, "id");
        if (!ID_FORMAT.matcher(id).matches()) {
            throw new IllegalArgumentException("Identifiant de fichier invalide");
        }
        Objects.requireNonNull(uploadedAt, "uploadedAt");
        contentType = requireSupported(contentType);
        filename = sanitize(filename);
        if (sizeBytes <= 0) {
            throw new InvalidInputException("Le fichier est vide");
        }
    }

    public static String requireSupported(String contentType) {
        String type = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT).split(";")[0].trim();
        if (!SUPPORTED_TYPES.contains(type)) {
            throw new InvalidInputException("Format non pris en charge. Attendu : MP4, WebM ou Ogg.");
        }
        return type;
    }

    /** L'adresse servie au navigateur. Elle ne contient que l'identifiant tiré au hasard. */
    public String url() {
        return "/api/media/" + id;
    }

    /**
     * Nom d'affichage seulement : jamais un chemin. Les séparateurs et les
     * caractères exotiques sont retirés, la longueur est bornée.
     */
    private static String sanitize(String filename) {
        if (filename == null || filename.isBlank()) {
            return "video";
        }
        String cleaned = filename.replaceAll("[\\\\/\\p{Cntrl}]", "").trim();
        cleaned = cleaned.isEmpty() ? "video" : cleaned;
        return cleaned.length() > MAX_FILENAME_LENGTH ? cleaned.substring(0, MAX_FILENAME_LENGTH) : cleaned;
    }
}
