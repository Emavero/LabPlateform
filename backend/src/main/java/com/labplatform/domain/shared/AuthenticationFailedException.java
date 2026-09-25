package com.labplatform.domain.shared;

/** Identifiants invalides. Message volontairement générique pour ne rien révéler. */
public class AuthenticationFailedException extends DomainException {

    public AuthenticationFailedException() {
        super("Adresse e-mail ou mot de passe invalide");
    }
}
