package com.labplatform.adapter.in.web;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le bouton « Démarrer / Arrêter » de bout en bout, en mode simulé — donc sans
 * projet GCP. Les délais de transition sont nuls sous le profil de test : la
 * machine passe d'un état stable à l'autre, ce qui laisse vérifier les refus
 * sans faire avancer d'horloge.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MachineApiIntegrationTest {

    private static final String COOKIE = "LAB_SESSION";

    @Autowired
    private MockMvc mvc;

    @Test
    void theThreeRoutesAreClosedToAnyoneWithoutASession() throws Exception {
        // La contrainte tient par la chaîne de sécurité, pas par une annotation
        // sur le contrôleur : c'est donc elle qu'on vérifie.
        mvc.perform(get("/api/machine/status")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/machine/start")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/machine/stop")).andExpect(status().isUnauthorized());
    }

    @Test
    void aLearnerStartsTheTargetAndSeesItsInternalAddress() throws Exception {
        Cookie session = register("machine1@example.com");

        mvc.perform(get("/api/machine/status").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINATED"))
                .andExpect(jsonPath("$.transitioning").value(false))
                .andExpect(jsonPath("$.internalIp").doesNotExist());

        mvc.perform(post("/api/machine/start").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.internalIp").value("10.10.10.10"));

        // Déjà allumée : Compute Engine accepterait sans rien faire, pas nous.
        mvc.perform(post("/api/machine/start").cookie(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("La machine est déjà en cours d'exécution"));

        mvc.perform(post("/api/machine/stop").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINATED"))
                .andExpect(jsonPath("$.internalIp").doesNotExist());

        mvc.perform(post("/api/machine/stop").cookie(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("La machine est déjà arrêtée"));
    }

    @Test
    void theStateIsLabelledInTheLanguageOfTheRequest() throws Exception {
        Cookie session = register("machine2@example.com");

        mvc.perform(get("/api/machine/status").cookie(session).header("Accept-Language", "fr"))
                .andExpect(jsonPath("$.statusName").value("Éteinte"));
        mvc.perform(get("/api/machine/status").cookie(session).header("Accept-Language", "en"))
                .andExpect(jsonPath("$.statusName").value("Switched off"));
    }

    private Cookie register(String email) throws Exception {
        String credentials = """
                {"email":"%s","password":"password123","confirmPassword":"password123"}""".formatted(email);
        MvcResult result = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(credentials)).andReturn();
        if (result.getResponse().getStatus() == 409) {
            result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                    .andExpect(status().isOk())
                    .andReturn();
        }
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        return new Cookie(COOKIE, setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';')));
    }
}
