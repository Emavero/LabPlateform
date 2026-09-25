package com.labplatform.adapter.in.web.security;

import java.util.Optional;

/** Vérifie un jeton de session reçu et en extrait l'utilisateur, ou vide s'il est invalide/expiré. */
public interface AccessTokenVerifier {

    Optional<AuthenticatedUser> verify(String token);
}
