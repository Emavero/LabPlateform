package com.labplatform.domain.support;

/**
 * Sujet d'une demande, choisi par le demandeur.
 * <p>
 * La catégorie n'aiguille pas la demande vers une équipe — il n'y en a qu'une —
 * mais dit à l'administration <em>où</em> la plateforme coince : dix demandes
 * d'abonnement et aucune sur les cours ne se lisent pas de la même façon.
 */
public enum TicketCategory {

    ACCOUNT("Compte"),
    BILLING("Abonnement"),
    MACHINES("Machines"),
    COURSES("Cours"),
    LAB("Infrastructure du lab"),
    OTHER("Autre");

    private final String displayName;

    TicketCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
