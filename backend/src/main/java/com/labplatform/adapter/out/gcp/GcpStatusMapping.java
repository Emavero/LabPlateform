package com.labplatform.adapter.out.gcp;

import com.labplatform.domain.lab.VmStatus;

import java.util.Locale;
import java.util.Map;

/**
 * Traduction des états de Compute Engine vers ceux du domaine.
 * <p>
 * Compute Engine en annonce une douzaine, le domaine en connaît cinq. Le
 * rapprochement est fait ici, une fois, plutôt que dispersé en conditions : ce
 * qui compte pour l'interface n'est pas la nuance entre « SUSPENDING » et
 * « STOPPING », mais le fait que la machine bouge et qu'il faudra redemander.
 */
final class GcpStatusMapping {

    private static final Map<String, VmStatus> KNOWN = Map.ofEntries(
            Map.entry("PROVISIONING", VmStatus.PROVISIONING),
            // L'instance attend des ressources : elle n'a pas commencé à démarrer.
            Map.entry("PENDING", VmStatus.PROVISIONING),
            // Réparation par l'hébergeur : la machine reviendra d'elle-même.
            Map.entry("REPAIRING", VmStatus.PROVISIONING),
            Map.entry("STAGING", VmStatus.STAGING),
            Map.entry("RUNNING", VmStatus.RUNNING),
            Map.entry("STOPPING", VmStatus.STOPPING),
            Map.entry("PENDING_STOP", VmStatus.STOPPING),
            Map.entry("SUSPENDING", VmStatus.STOPPING),
            Map.entry("DEPROVISIONING", VmStatus.STOPPING),
            Map.entry("TERMINATED", VmStatus.TERMINATED),
            Map.entry("STOPPED", VmStatus.TERMINATED),
            // Suspendue : éteinte de notre point de vue, elle se rallume de la
            // même façon. La nuance de facturation ne regarde pas l'apprenant.
            Map.entry("SUSPENDED", VmStatus.TERMINATED));

    private GcpStatusMapping() {
    }

    /**
     * État du domaine correspondant.
     * <p>
     * Un état inconnu est traité comme un passage plutôt que comme une panne :
     * Google ajoute des états au fil du temps, et refuser d'afficher la page
     * pour un mot nouveau serait disproportionné. L'appelant journalise.
     */
    static VmStatus of(String gcpStatus) {
        if (gcpStatus == null) {
            return VmStatus.PROVISIONING;
        }
        return KNOWN.getOrDefault(gcpStatus.trim().toUpperCase(Locale.ROOT), VmStatus.PROVISIONING);
    }

    static boolean isKnown(String gcpStatus) {
        return gcpStatus != null && KNOWN.containsKey(gcpStatus.trim().toUpperCase(Locale.ROOT));
    }
}
