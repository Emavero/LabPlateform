package com.labplatform.domain.journal;

/**
 * Grande famille d'usage d'un événement.
 * <p>
 * Elle existe pour répondre à une question que le simple décompte de comptes
 * ne permet pas : <em>à quoi les gens se servent-ils de la plateforme ?</em>
 * En classant chaque compte par la famille où il agit le plus, on obtient des
 * profils d'usage — celui qui attaque des machines, celui qui apprend, celui
 * qui n'a encore rien fait — et donc ce qu'il faut leur proposer ensuite.
 */
public enum JournalFamily {

    ACCOUNT("Compte"),
    MACHINES("Machines"),
    ACADEMY("Cours"),
    LAB("Infrastructure"),
    BILLING("Abonnement"),
    SUPPORT("Support");

    private final String displayName;

    JournalFamily(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
