package com.labplatform.domain.academy;

import com.labplatform.domain.shared.NotFoundException;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Sous-domaine d'une filière : ce dont le cours traite précisément.
 * <p>
 * Chaque sous-domaine connaît sa filière, et non l'inverse : un cours ne
 * déclare donc que son sous-domaine, sa filière s'en déduit. Deux données qui
 * pourraient se contredire n'existent pas — un cours de « Analyse mémoire »
 * rangé en Défense est impossible à écrire.
 * <p>
 * Ajouter un sous-domaine revient à ajouter une constante ici : la page de
 * cours, ses filtres et l'éditeur d'administration le proposent aussitôt.
 */
public enum CourseTopic {

    EVIDENCE_HANDLING(Track.FORENSICS, "Collecte et preuve", "collecte-et-preuve"),
    MEMORY_ANALYSIS(Track.FORENSICS, "Analyse mémoire", "analyse-memoire"),
    DISK_FORENSICS(Track.FORENSICS, "Analyse de disque", "analyse-de-disque"),
    LOG_ANALYSIS(Track.FORENSICS, "Analyse de journaux", "analyse-de-journaux"),
    NETWORK_FORENSICS(Track.FORENSICS, "Forensique réseau", "forensique-reseau"),
    TIMELINE(Track.FORENSICS, "Chronologie d'incident", "chronologie-d-incident"),
    MALWARE_ANALYSIS(Track.FORENSICS, "Analyse de maliciel", "analyse-de-maliciel"),

    HARDENING(Track.DEFENSE, "Durcissement", "durcissement"),
    SIEM_SOC(Track.DEFENSE, "SIEM et SOC", "siem-et-soc"),
    FIREWALLS(Track.DEFENSE, "Pare-feux", "pare-feux"),
    INCIDENT_RESPONSE(Track.DEFENSE, "Réponse à incident", "reponse-a-incident"),
    THREAT_HUNTING(Track.DEFENSE, "Chasse aux menaces", "chasse-aux-menaces"),
    IDENTITY_ACCESS(Track.DEFENSE, "Identités et accès", "identites-et-acces");

    private final Track track;
    private final String displayName;
    private final String slug;

    CourseTopic(Track track, String displayName, String slug) {
        this.track = track;
        this.displayName = displayName;
        this.slug = slug;
    }

    /** Sous-domaines d'une filière, dans l'ordre où ils se présentent. */
    public static List<CourseTopic> of(Track track) {
        return Arrays.stream(values()).filter(topic -> topic.track == track).toList();
    }

    public static CourseTopic ofSlug(String slug) {
        String wanted = slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(topic -> topic.slug.equals(wanted))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Sous-domaine introuvable"));
    }

    public Track track() {
        return track;
    }

    public String displayName() {
        return displayName;
    }

    public String slug() {
        return slug;
    }
}
