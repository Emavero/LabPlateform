package com.labplatform.domain.shared;

/**
 * Racine de toutes les erreurs métier. Le domaine ne connaît pas HTTP :
 * c'est l'adaptateur web qui traduit chaque catégorie en code de statut.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
