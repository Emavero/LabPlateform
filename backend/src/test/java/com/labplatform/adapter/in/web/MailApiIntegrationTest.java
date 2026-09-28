package com.labplatform.adapter.in.web;

import com.labplatform.adapter.out.notification.SmtpPasswordResetNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Envoi du lien de réinitialisation, serveur SMTP branché.
 * <p>
 * Le serveur est remplacé par un double qui retient les messages — et qui sait
 * tomber en panne, car c'est le comportement en panne qui importe : une erreur
 * d'envoi ne doit pas se voir dans la réponse HTTP, sinon elle trahirait
 * quelles adresses ont un compte.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.mail.enabled=true",
        "app.security.expose-reset-token-in-response=false",
        "app.public-url=https://lab.example.org",
})
class MailApiIntegrationTest {

    private static final String EMAIL = "destinataire-mail@example.com";

    /** Double du serveur d'envoi : il retient, ou il tombe en panne sur commande. */
    static class RecordingMailSender extends JavaMailSenderImpl {

        final List<SimpleMailMessage> sent = new ArrayList<>();
        boolean failing = false;

        @Override
        public void send(SimpleMailMessage... messages) {
            if (failing) {
                throw new MailSendException("serveur injoignable");
            }
            sent.addAll(List.of(messages));
        }
    }

    @TestConfiguration
    static class MailStub {

        @Bean
        @Primary
        JavaMailSender javaMailSender() {
            return new RecordingMailSender();
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JavaMailSender mailer;

    @Autowired
    private ApplicationContext context;

    private RecordingMailSender recorder;

    @BeforeEach
    void reset() {
        recorder = (RecordingMailSender) mailer;
        recorder.sent.clear();
        recorder.failing = false;
    }

    private void register(String email) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"password123\","
                        + "\"confirmPassword\":\"password123\"}"));
    }

    private void forget(String email) throws Exception {
        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk())
                // Le jeton ne sort plus dans la réponse : il part par courriel.
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    /** Avec l'envoi actif, c'est l'adaptateur SMTP qui tient le port, pas le journal. */
    @Test
    void lAdaptateurSmtpRemplaceLeNotificateurDeJournal() {
        assertNotNull(context.getBean(SmtpPasswordResetNotifier.class));
        assertEquals(0, context.getBeanNamesForType(
                com.labplatform.adapter.out.notification.LoggingPasswordResetNotifier.class).length);
    }

    @Test
    void leLienDeReinitialisationPartParCourriel() throws Exception {
        register(EMAIL);

        forget(EMAIL);

        assertEquals(1, recorder.sent.size());
        SimpleMailMessage message = recorder.sent.get(0);
        assertEquals(EMAIL, message.getTo()[0]);
        assertTrue(message.getSubject().contains("réinitialisation"), message.getSubject());
        assertTrue(message.getText().contains("https://lab.example.org/reset-password?token="), message.getText());
    }

    /** Une adresse inconnue ne déclenche aucun envoi, et répond comme les autres. */
    @Test
    void uneAdresseInconnueNeDeclencheAucunEnvoi() throws Exception {
        forget("personne-ici@example.com");

        assertTrue(recorder.sent.isEmpty());
    }

    /**
     * Le cœur de la règle : une panne du serveur d'envoi ne doit pas se voir.
     * Si elle produisait une erreur 500, celle-ci ne surviendrait que pour les
     * adresses connues — et révélerait donc qui a un compte.
     */
    @Test
    void unePanneDEnvoiNeSeVoitPasDansLaReponse() throws Exception {
        register("panne-mail@example.com");
        recorder.failing = true;

        forget("panne-mail@example.com");

        assertTrue(recorder.sent.isEmpty());
    }

    @Test
    void laLangueDeLaRequeteDecideDeCelleDuMessage() throws Exception {
        register("langue-mail@example.com");

        mvc.perform(post("/api/auth/forgot-password").header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"langue-mail@example.com\"}"))
                .andExpect(status().isOk());

        SimpleMailMessage message = recorder.sent.get(0);
        assertTrue(message.getSubject().contains("reset your password"), message.getSubject());
        assertFalse(message.getText().contains("Bonjour"));
    }
}
