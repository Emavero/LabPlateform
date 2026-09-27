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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Surface d'attaque servie par l'API.
 * <p>
 * Le test qui compte est celui de l'abonnement : ce module lit tout le
 * catalogue d'un coup, il ne doit pas devenir le moyen d'obtenir l'adresse
 * d'une cible réservée sans payer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExposureApiIntegrationTest {

    private static final String COOKIE = "LAB_SESSION";
    private static final String OPEN_SLUG = "exposition-libre";
    private static final String PRO_SLUG = "exposition-reservee";
    private static final String PRO_ADDRESS = "10.10.77.21";
    private static final String USER_FLAG = "4f4e4d4c4b4a49484746454443424140";
    private static final String ROOT_FLAG = "5f5e5d5c5b5a59585756555453525150";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private BoxRepositoryPort boxes;

    @BeforeEach
    void seedTargets() {
        if (boxes.findBySlug(OPEN_SLUG).isEmpty()) {
            boxes.save(Box.create(OPEN_SLUG, "Exposition Libre", OperatingSystem.LINUX, Difficulty.VERY_EASY,
                    "Ouverte à tous.", "10.10.77.20", "cyberMans", Instant.parse("2026-01-01T00:00:00Z"),
                    false, false, Flag.ofSecret(USER_FLAG), Flag.ofSecret(ROOT_FLAG)));
        }
        if (boxes.findBySlug(PRO_SLUG).isEmpty()) {
            boxes.save(Box.create(PRO_SLUG, "Exposition Réservée", OperatingSystem.WINDOWS, Difficulty.HARD,
                    "Réservée aux abonnés.", PRO_ADDRESS, "cyberMans", Instant.parse("2026-01-02T00:00:00Z"),
                    false, true, Flag.ofSecret(USER_FLAG), Flag.ofSecret(ROOT_FLAG)));
        }
    }

    @Test
    void lAnalyseNoteLesCiblesEtTraceDesChemins() throws Exception {
        Cookie session = register("exposition-libre@example.com");

        mvc.perform(get("/api/exposure").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targets.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$.targets[0].score").value(greaterThan(0)))
                .andExpect(jsonPath("$.targets[0].signals.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$.paths.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$.segments.length()").value(greaterThan(0)));
    }

    /** Sans abonnement : la cible réservée est listée, son adresse non. */
    @Test
    void sansAbonnementLAdresseDUneCibleReserveeNeSortPas() throws Exception {
        Cookie session = register("exposition-sans-abo@example.com");

        mvc.perform(get("/api/exposure").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unlocked").value(false))
                .andExpect(jsonPath("$.lockedOut").value(greaterThan(0)))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString(PRO_ADDRESS))));
    }

    @Test
    void lAdministrationVoitLeDetailSansSAbonner() throws Exception {
        Cookie session = register("admin@example.com");

        mvc.perform(get("/api/exposure").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unlocked").value(true))
                .andExpect(jsonPath("$.lockedOut").value(0))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(PRO_ADDRESS)));
    }

    @Test
    void laSurfaceNEstPasLisibleSansSession() throws Exception {
        mvc.perform(get("/api/exposure")).andExpect(status().isUnauthorized());
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
