package com.labplatform.domain.shared;

/** Opération incompatible avec l'état actuel (e-mail déjà pris, VM déjà démarrée...). */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }
}
