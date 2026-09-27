package com.labplatform.adapter.in.web;

import com.labplatform.application.port.out.BoxRepositoryPort;
import com.labplatform.domain.box.Box;
import com.labplatform.domain.box.Difficulty;
import com.labplatform.domain.box.Flag;
import com.labplatform.domain.lab.OperatingSystem;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Scénarios d'exercice à travers les adaptateurs réels.
 * <p>
 * Le point que les tests en mémoire ne couvrent pas est le remplacement des
 * étapes : l'éditeur envoie la liste entière, et la réécriture doit vider
 * l'ancienne sans laisser d'orphelines.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScenarioApiIntegrationTest {

    private static final String COOKIE = "LAB_SESSION";
    private static final String FIRST_SLUG = "scenario-cible-une";
    private static final String SECOND_SLUG = "scenario-cible-deux";
    private static final String USER_FLAG = "afaeadacabaaa9a8a7a6a5a4a3a2a1a0";
    private static final String ROOT_FLAG = "bfbebdbcbbbab9b8b7b6b5b4b3b2b1b0";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private BoxRepositoryPort boxes;

    @BeforeEach
    void seedTargets() {
        if (boxes.findBySlug(FIRST_SLUG).isEmpty()) {
            boxes.save(Box.create(FIRST_SLUG, "Cible Une", OperatingSystem.LINUX, Difficulty.EASY, "Synopsis.",
                    "10.10.88.10", "cyberMans", Instant.parse("2026-01-01T00:00:00Z"), false, false,
                    Flag.ofSecret(USER_FLAG), Flag.ofSecret(ROOT_FLAG)));
        }
        if (boxes.findBySlug(SECOND_SLUG).isEmpty()) {
            boxes.save(Box.create(SECOND_SLUG, "Cible Deux", OperatingSystem.LINUX, Difficulty.MEDIUM, "Synopsis.",
                    "10.10.88.11", "cyberMans", Instant.parse("2026-01-02T00:00:00Z"), false, false,
                    Flag.ofSecret(USER_FLAG), Flag.ofSecret(ROOT_FLAG)));
        }
    }

    private String body(String title, boolean published, String... references) {
        StringBuilder steps = new StringBuilder();
        for (String reference : references) {
            if (steps.length() > 0) {
                steps.append(',');
            }
            steps.append("{\"kind\":\"MACHINE\",\"reference\":\"").append(reference)
                    .append("\",\"instruction\":\"Consigne.\",\"objective\":\"USER_FLAG\"}");
        }
        return "{\"title\":\"" + title + "\",\"brief\":\"Mise en situation.\",\"published\":" + published
                + ",\"steps\":[" + steps + "]}";
    }

    @Test
    void laReecritureRemplaceLesEtapesSansEnLaisserDOrphelines() throws Exception {
        Cookie staff = register("admin@example.com");

        String slug = slug(mvc.perform(post("/api/admin/scenarios").cookie(staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Chaîne complète", true, FIRST_SLUG, SECOND_SLUG)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        mvc.perform(get("/api/scenarios/" + slug).cookie(staff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps.length()").value(2))
                .andExpect(jsonPath("$.steps[0].position").value(1))
                .andExpect(jsonPath("$.steps[1].position").value(2));

        mvc.perform(put("/api/admin/scenarios/" + slug).cookie(staff).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Chaîne complète", true, SECOND_SLUG)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/scenarios/" + slug).cookie(staff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps.length()").value(1))
                .andExpect(jsonPath("$.steps[0].reference").value(SECOND_SLUG))
                .andExpect(jsonPath("$.steps[0].position").value(1))
                .andExpect(jsonPath("$.steps[0].name").value("Cible Deux"));

        mvc.perform(delete("/api/admin/scenarios/" + slug).cookie(staff)).andExpect(status().isNoContent());
        mvc.perform(get("/api/scenarios/" + slug).cookie(staff)).andExpect(status().isNotFound());
    }

    @Test
    void unBrouillonNEstPasVisibleDunJoueur() throws Exception {
        Cookie staff = register("admin@example.com");
        Cookie player = register("scenario-joueur@example.com");

        String slug = slug(mvc.perform(post("/api/admin/scenarios").cookie(staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Brouillon caché", false, FIRST_SLUG)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        mvc.perform(get("/api/scenarios/" + slug).cookie(player)).andExpect(status().isNotFound());
        mvc.perform(get("/api/scenarios/" + slug).cookie(staff)).andExpect(status().isOk());

        mvc.perform(delete("/api/admin/scenarios/" + slug).cookie(staff)).andExpect(status().isNoContent());
    }

    @Test
    void laConceptionEstRefuseeAUnJoueur() throws Exception {
        Cookie player = register("scenario-refus@example.com");

        mvc.perform(get("/api/admin/scenarios").cookie(player)).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/scenarios").cookie(player).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Interdit", true, FIRST_SLUG)))
                .andExpect(status().isForbidden());
    }

    @Test
    void uneReferenceInconnueEstRefusee() throws Exception {
        Cookie staff = register("admin@example.com");

        mvc.perform(post("/api/admin/scenarios").cookie(staff).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Étape fantôme", true, "machine-qui-nexiste-pas")))
                .andExpect(status().isBadRequest());
    }

    private static String slug(String json) {
        int start = json.indexOf("\"slug\":\"") + 8;
        return json.substring(start, json.indexOf('"', start));
    }

    private Cookie register(String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\","
                                + "\"confirmPassword\":\"password123\"}"))
                .andReturn();
        if (result.getResponse().getStatus() != 201) {
            result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                    .andExpect(status().isOk())
                    .andReturn();
        }
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        return new Cookie(COOKIE, setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';')));
    }
}
