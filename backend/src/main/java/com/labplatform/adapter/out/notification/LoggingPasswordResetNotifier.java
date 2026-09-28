package com.labplatform.adapter.out.notification;

import com.labplatform.application.port.out.PasswordResetNotifierPort;
import com.labplatform.domain.user.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Notificateur de remplacement tant qu'aucun serveur d'e-mail n'est branché.
 * Il ne journalise jamais le jeton lui-même.
 * <p>
 * Il s'efface dès que {@code app.mail.enabled} passe à vrai : c'est alors
 * {@link SmtpPasswordResetNotifier} qui tient le port. Les deux ne coexistent
 * jamais — une seule implémentation par port, sinon le contexte ne saurait
 * laquelle injecter.
 */
@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingPasswordResetNotifier implements PasswordResetNotifierPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetNotifier.class);

    @Override
    public void sendResetLink(Email recipient, String rawToken, Instant expiresAt) {
        log.info("Réinitialisation de mot de passe demandée pour {} (lien valable jusqu'à {}). "
                + "Aucun envoi d'e-mail configuré.", recipient, expiresAt);
    }
}
