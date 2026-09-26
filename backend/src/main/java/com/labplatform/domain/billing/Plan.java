package com.labplatform.domain.billing;

/**
 * Formule d'un compte. Le catalogue de machines se consulte en {@link #FREE},
 * mais les informations qui servent à attaquer une cible (adresse, cible à la
 * demande, comptes rendus) sont réservées à {@link #PRO}.
 */
public enum Plan {

    FREE("Découverte"),
    PRO("Pro");

    private final String displayName;

    Plan(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isPro() {
        return this == PRO;
    }
}
