package com.labplatform.application.port.in.auth;

/** Résultat d'une inscription ou d'une connexion : l'utilisateur et son jeton de session. */
public record AuthenticatedSession(UserSummary user, String accessToken) {
}
