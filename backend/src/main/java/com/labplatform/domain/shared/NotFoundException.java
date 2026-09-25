package com.labplatform.domain.shared;

/** Ressource absente, ou invisible pour l'acteur qui la demande. */
public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(message);
    }
}
