package com.labplatform.auth;

/**
 * devResetToken n'est renseigné qu'en environnement de développement
 * (voir AuthService) : en l'absence de serveur d'e-mail, il permet de tester
 * le flux de réinitialisation de bout en bout depuis le frontend.
 * À retirer / masquer dès qu'un vrai envoi d'e-mail est branché en production.
 */
public record ForgotPasswordResponse(
        String message,
        String devResetToken
) {
}
