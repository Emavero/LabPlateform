package com.labplatform.adapter.in.web;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La langue de la réponse suit l'en-tête {@code Accept-Language} de la requête.
 * <p>
 * Ce test passe par la vraie chaîne HTTP, car c'est elle qui alimente la langue
 * du fil : une traduction qui marche en appel direct mais pas derrière un
 * contrôleur ne sert à rien.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LocalisationApiIntegrationTest {

    private static final String COOKIE = "LAB_SESSION";

    @Autowired
    private MockMvc mvc;

    @Test
    void anErrorComesBackInFrenchByDefault() throws Exception {
        Cookie session = register("langue-fr@example.com");

        mvc.perform(get("/api/boxes/{slug}", "machine-inexistante").cookie(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Machine introuvable"));
    }

    @Test
    void theSameErrorComesBackInEnglishWhenAsked() throws Exception {
        Cookie session = register("langue-en@example.com");

        mvc.perform(get("/api/boxes/{slug}", "machine-inexistante").cookie(session)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Machine not found"));
    }

    /** Les libellés d'affichage suivent la même règle que les messages d'erreur. */
    @Test
    void displayLabelsFollowTheRequestedLanguage() throws Exception {
        Cookie session = register("libelles@example.com");

        mvc.perform(get("/api/scoreboard/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rankName").value("Noob"));

        mvc.perform(get("/api/courses/tracks").cookie(session)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.track == 'FORENSICS')].name").value("Forensics"));

        mvc.perform(get("/api/courses/tracks").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.track == 'FORENSICS')].name").value("Forensique"));
    }

    /** Une langue que nous ne servons pas retombe sur le français, pas sur du vide. */
    @Test
    void anUnservedLanguageFallsBackToFrench() throws Exception {
        Cookie session = register("langue-inconnue@example.com");

        mvc.perform(get("/api/boxes/{slug}", "machine-inexistante").cookie(session)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "de"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Machine introuvable"));
    }

    /** Un message de validation, construit par le cadre, se traduit aussi. */
    @Test
    void validationMessagesAreTranslatedToo() throws Exception {
        Cookie session = register("validation@example.com");

        mvc.perform(post("/api/boxes/{slug}/flags", "sentinel").cookie(session)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"USER\",\"flag\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The flag is required"));
    }

    private Cookie register(String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\","
                                + "\"confirmPassword\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        return new Cookie(COOKIE, setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';')));
    }
}
