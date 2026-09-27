package com.labplatform.domain.insight;

import com.labplatform.domain.journal.JournalFamily;

/**
 * Profil d'usage d'un compte, déduit de la famille où il agit le plus.
 * <p>
 * C'est la réponse à « qui sont mes utilisateurs ? » quand le nombre de
 * comptes ne dit rien. Un compte qui valide des flags et un compte qui suit
 * des cours n'attendent pas la même chose de la plateforme, et ne se relancent
 * pas de la même façon.
 */
public enum UsageProfile {

    /** Attaque des machines : c'est le cœur de cible, et le plus enclin à s'abonner. */
    HUNTER("Chasseur de machines", "Propose-lui les machines du moment et les comptes rendus."),
    /** Suit des cours : à conduire vers une première machine d'initiation. */
    LEARNER("Apprenant", "Conduis-le vers une machine d'initiation en fin de filière."),
    /** Monte son infrastructure : VPN, machines d'attaque. */
    BUILDER("Bâtisseur de lab", "Documente le lab et propose des scénarios à rejouer."),
    /** Regarde sans agir : le contenu attire, quelque chose retient. */
    BROWSER("Curieux", "Il regarde sans agir : vérifie ce qui le retient au premier pas."),
    /** N'a rien fait depuis son inscription. */
    DORMANT("Dormant", "Rien depuis l'inscription : une relance ou un premier pas guidé.");

    private final String displayName;
    private final String advice;

    UsageProfile(String displayName, String advice) {
        this.displayName = displayName;
        this.advice = advice;
    }

    public String displayName() {
        return displayName;
    }

    public String advice() {
        return advice;
    }

    /**
     * Profil correspondant à la famille dominante. La consultation seule ne
     * fait pas un chasseur : c'est l'appelant qui distingue « a agi » de
     * « a seulement regardé », et passe {@code null} dans le second cas.
     */
    public static UsageProfile of(JournalFamily dominant) {
        if (dominant == null) {
            return DORMANT;
        }
        return switch (dominant) {
            case MACHINES -> HUNTER;
            case ACADEMY -> LEARNER;
            case LAB -> BUILDER;
            // Payer ou écrire au support n'est pas un usage en soi : ce sont
            // des actes autour de l'usage, pas ce qu'on vient chercher.
            case BILLING, SUPPORT, ACCOUNT -> BROWSER;
        };
    }
}
