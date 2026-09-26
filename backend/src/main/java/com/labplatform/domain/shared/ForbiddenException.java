package com.labplatform.domain.shared;

/** Opération légitime, mais interdite à l'appelant (rôle insuffisant). */
public class ForbiddenException extends DomainException {

    public ForbiddenException(String message) {
        super(message);
    }
}
