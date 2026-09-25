package com.labplatform.application.port.out;

import com.labplatform.domain.user.Email;

import java.time.Instant;

/** Achemine le lien de réinitialisation à l'utilisateur (e-mail, SMS...). */
public interface PasswordResetNotifierPort {

    void sendResetLink(Email recipient, String rawToken, Instant expiresAt);
}
