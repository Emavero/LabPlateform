package com.labplatform.application.port.out;

import com.labplatform.application.port.in.auth.UserSummary;

/** Émet le jeton de session d'un utilisateur authentifié (JWT dans l'implémentation actuelle). */
public interface AccessTokenIssuerPort {

    String issue(UserSummary user);
}
