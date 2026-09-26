package com.labplatform.domain.journal;

/**
 * Nature d'un événement du journal.
 * <p>
 * Ne sont inscrits que des actes réels, faits côté serveur : aucune balise de
 * suivi dans le navigateur, aucun pixel, aucune position de souris. Ce que la
 * plateforme sait d'un compte est donc exactement ce que ce compte a demandé
 * à la plateforme — et cela suffit à savoir ce qui sert et ce qui bloque.
 */
public enum JournalKind {

    /**
     * Seule nature de la famille « compte » : la date d'inscription est déjà
     * portée par le compte lui-même, et compter les connexions n'apprend rien
     * sur ce dont quelqu'un a besoin.
     */
    REGISTERED(JournalFamily.ACCOUNT, "Inscription"),

    BOX_VIEWED(JournalFamily.MACHINES, "Machine consultée"),
    BOX_SPAWNED(JournalFamily.MACHINES, "Cible lancée"),
    BOX_STOPPED(JournalFamily.MACHINES, "Cible arrêtée"),
    FLAG_VALIDATED(JournalFamily.MACHINES, "Flag validé"),
    FLAG_REFUSED(JournalFamily.MACHINES, "Flag refusé"),
    BOX_PWNED(JournalFamily.MACHINES, "Machine possédée"),
    BOX_RATED(JournalFamily.MACHINES, "Difficulté notée"),
    WRITEUP_PUBLISHED(JournalFamily.MACHINES, "Compte rendu publié"),
    /** Refus faute d'abonnement : le signal le plus direct d'une envie non servie. */
    BOX_LOCKED_OUT(JournalFamily.MACHINES, "Machine réservée atteinte"),

    COURSE_VIEWED(JournalFamily.ACADEMY, "Cours consulté"),
    SECTION_COMPLETED(JournalFamily.ACADEMY, "Section terminée"),
    QUIZ_PASSED(JournalFamily.ACADEMY, "Quiz réussi"),
    QUIZ_FAILED(JournalFamily.ACADEMY, "Quiz manqué"),

    VM_STARTED(JournalFamily.LAB, "Machine d'attaque démarrée"),
    VPN_PROFILE_ISSUED(JournalFamily.LAB, "Profil VPN émis"),

    CHECKOUT_STARTED(JournalFamily.BILLING, "Paiement engagé"),
    SUBSCRIPTION_STARTED(JournalFamily.BILLING, "Abonnement ouvert"),
    SUBSCRIPTION_CANCELLED(JournalFamily.BILLING, "Abonnement résilié"),
    PAYMENT_FAILED(JournalFamily.BILLING, "Paiement échoué"),

    TICKET_OPENED(JournalFamily.SUPPORT, "Demande ouverte"),
    TICKET_REPLIED(JournalFamily.SUPPORT, "Réponse au support");

    private final JournalFamily family;
    private final String displayName;

    JournalKind(JournalFamily family, String displayName) {
        this.family = family;
        this.displayName = displayName;
    }

    public JournalFamily family() {
        return family;
    }

    public String displayName() {
        return displayName;
    }

    /**
     * Événement qui marque un usage volontaire de la plateforme. La connexion
     * et l'inscription n'en font pas partie : ouvrir une session n'apprend rien
     * sur ce dont un compte a besoin.
     */
    public boolean isEngagement() {
        return family != JournalFamily.ACCOUNT;
    }
}
