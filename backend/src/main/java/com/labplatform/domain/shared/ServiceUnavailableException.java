package com.labplatform.domain.shared;

/** L'infrastructure nécessaire à l'opération (hyperviseur, conteneurs...) ne répond pas. */
public class ServiceUnavailableException extends DomainException {

    public ServiceUnavailableException(String message) {
        super(message);
    }
}
