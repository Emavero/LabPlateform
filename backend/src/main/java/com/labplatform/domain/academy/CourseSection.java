package com.labplatform.domain.academy;

import com.labplatform.domain.shared.InvalidInputException;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Section d'un cours : l'unité que l'apprenant coche une fois terminée.
 *
 * @param position rang dans le cours, à partir de 1
 * @param minutes  durée indicative, qui alimente la durée totale du cours
 * @param videoUrl vidéo d'accompagnement, nulle pour une section sans vidéo
 */
public record CourseSection(Long id, String slug, String title, SectionKind kind, int position, int minutes,
                            String content, String videoUrl) {

    private static final Pattern SLUG_FORMAT = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    private static final int MAX_VIDEO_URL_LENGTH = 512;

    /** Section sans vidéo. */
    public static CourseSection of(Long id, String slug, String title, SectionKind kind, int position, int minutes,
                                   String content) {
        return new CourseSection(id, slug, title, kind, position, minutes, content, null);
    }

    public CourseSection {
        if (slug == null || !SLUG_FORMAT.matcher(slug).matches()) {
            throw new InvalidInputException("Identifiant de section invalide");
        }
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(content, "content");
        if (position < 1) {
            throw new IllegalArgumentException("La première section porte le numéro 1");
        }
        if (minutes < 0) {
            throw new IllegalArgumentException("Une durée négative n'a pas de sens : " + minutes);
        }
        videoUrl = normalizeVideoUrl(videoUrl);
    }

    public boolean hasVideo() {
        return videoUrl != null;
    }

    /**
     * Une vidéo est une adresse http(s), rien d'autre : ni « javascript: »,
     * ni « data: », qui s'exécuteraient dans la page de l'apprenant.
     */
    private static String normalizeVideoUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String url = raw.trim();
        if (url.length() > MAX_VIDEO_URL_LENGTH) {
            throw new InvalidInputException("L'adresse de la vidéo est trop longue");
        }
        String lowered = url.toLowerCase(Locale.ROOT);
        if (!lowered.startsWith("https://") && !lowered.startsWith("http://")) {
            throw new InvalidInputException("L'adresse de la vidéo doit commencer par https://");
        }
        return url;
    }
}
