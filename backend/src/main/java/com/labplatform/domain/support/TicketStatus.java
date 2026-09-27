package com.labplatform.domain.support;

/**
 * Où en est une demande d'assistance.
 * <p>
 * Trois états suffisent, et ils disent chacun <em>de qui on attend quelque
 * chose</em> : {@link #OPEN} de l'équipe, {@link #ANSWERED} du demandeur,
 * {@link #RESOLVED} de personne. Une file d'attente se lit alors sans
 * interprétation : ce qui est ouvert est ce qui reste à faire.
 */
public enum TicketStatus {

    OPEN("Ouverte"),
    ANSWERED("Répondu"),
    RESOLVED("Résolue");

    private final String displayName;

    TicketStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /** Demande qui attend encore l'équipe : c'est ce qui compose sa file. */
    public boolean isWaitingOnStaff() {
        return this == OPEN;
    }
}
