package com.labplatform.domain.shared;

/** Donnée d'entrée qui viole une règle métier (format d'e-mail, politique de mot de passe...). */
public class InvalidInputException extends DomainException {

    public InvalidInputException(String message) {
        super(message);
    }
}
