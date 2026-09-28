package com.labplatform.adapter.out.notification;

import com.labplatform.application.port.out.PasswordResetNotifierPort;
import com.labplatform.config.AppProperties;
import com.labplatform.domain.user.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * Envoi du lien de réinitialisation par SMTP.
 * <p>
 * Deux précautions portent tout l'intérêt de cet adaptateur.
 * <p>
 * <b>Un échec d'envoi ne remonte jamais.</b> Le service répond la même phrase
 * que l'adresse existe ou non, précisément pour ne pas révéler qui a un compte
 * ici. Laisser une panne SMTP produire une erreur 500 réintroduirait la fuite
 * par la bande : l'erreur ne surviendrait que pour les adresses connues. La
 * panne est donc journalisée, et l'appelant reçoit la réponse habituelle.
 * <p>
 * <b>Le jeton n'apparaît dans aucun journal.</b> Il vaut un mot de passe le
 * temps de sa validité ; il ne voyage que dans le corps du message.
 * <p>
 * La langue est celle de la requête qui a demandé la réinitialisation : c'est
 * la seule que la plateforme connaisse à cet instant, et elle est
 * vraisemblablement celle que la personne lit.
 */
@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class SmtpPasswordResetNotifier implements PasswordResetNotifierPort {

    private static final Logger log = LoggerFactory.getLogger(SmtpPasswordResetNotifier.class);

    private final JavaMailSender mailer;
    private final Clock clock;
    private final String from;
    private final String publicUrl;

    public SmtpPasswordResetNotifier(JavaMailSender mailer, Clock clock, AppProperties properties) {
        this.mailer = mailer;
        this.clock = clock;
        this.from = properties.getMail().getFrom();
        this.publicUrl = properties.getPublicUrl();
    }

    @Override
    public void sendResetLink(Email recipient, String rawToken, Instant expiresAt) {
        ResetMailContent content = ResetMailContent.of(publicUrl, rawToken, expiresAt, clock.instant(),
                LocaleContextHolder.getLocale());

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient.value());
        message.setSubject(content.subject());
        message.setText(content.body());

        try {
            mailer.send(message);
            log.info("Lien de réinitialisation envoyé à {}.", recipient);
        } catch (MailException e) {
            // Volontairement avalé : voir la note de classe.
            log.error("Envoi du lien de réinitialisation impossible pour {} : {}", recipient, e.getMessage());
        }
    }
}
