package com.labplatform.domain.academy;

import com.labplatform.domain.shared.NotFoundException;

import java.util.Arrays;
import java.util.Locale;

/**
 * Filière de cours. Ajouter une filière revient à ajouter une constante
 * ici : ni le menu, ni les cas d'usage, ni l'API n'ont à changer.
 */
public enum Track {

    FORENSICS("Forensique", "forensique",
            "Analyse post-incident : collecte de traces, mémoire, disques, journaux, chronologie."),
    DEFENSE("Défense", "defense",
            "Durcissement, détection et réponse : surveiller, contenir et fermer les portes.");

    private final String displayName;
    private final String slug;
    private final String description;

    Track(String displayName, String slug, String description) {
        this.displayName = displayName;
        this.slug = slug;
        this.description = description;
    }

    public static Track ofSlug(String slug) {
        String wanted = slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(track -> track.slug.equals(wanted))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Filière introuvable"));
    }

    public String displayName() {
        return displayName;
    }

    public String slug() {
        return slug;
    }

    public String description() {
        return description;
    }
}
