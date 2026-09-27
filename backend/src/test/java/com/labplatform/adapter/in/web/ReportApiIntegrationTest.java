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

/** Rapport d'activité servi par l'API, à travers les adaptateurs réels. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportApiIntegrationTest {

    private static final String COOKIE = "LAB_SESSION";

    @Autowired
    private MockMvc mvc;

    /** Un compte neuf n'a rien obtenu, mais son inscription est déjà un acte. */
    @Test
    void unCompteNeufObtientUnRapportSansGainMaisComplet() throws Exception {
        Cookie session = register("rapport-neuf@example.com");

        mvc.perform(get("/api/reports/activity").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days").value(30))
                .andExpect(jsonPath("$.totals.flags").value(0))
                .andExpect(jsonPath("$.totals.boxesPwned").value(0))
                .andExpect(jsonPath("$.totals.sections").value(0))
                // Aucun quiz rendu : la part de réussite est absente, et non nulle.
                .andExpect(jsonPath("$.totals.quizSuccessPercent").doesNotExist())
                .andExpect(jsonPath("$.previous.flags").value(0))
                .andExpect(jsonPath("$.machines.length()").value(0));
    }

    @Test
    void laPeriodeDemandeeEstRameneeDansSesBornes() throws Exception {
        Cookie session = register("rapport-bornes@example.com");

        mvc.perform(get("/api/reports/activity").param("days", "1").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days").value(7));
        mvc.perform(get("/api/reports/activity").param("days", "9000").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days").value(365));
    }

    /** L'inscription est inscrite au journal : elle doit apparaître dans le rapport. */
    @Test
    void leRapportRelateLInscriptionDuCompte() throws Exception {
        Cookie session = register("rapport-inscription@example.com");

        mvc.perform(get("/api/reports/activity").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.events").value(1))
                .andExpect(jsonPath("$.totals.activeDays").value(1))
                .andExpect(jsonPath("$.families[0].code").value("ACCOUNT"))
                .andExpect(jsonPath("$.kinds[0].code").value("REGISTERED"));
    }

    @Test
    void leRapportNEstPasLisibleSansSession() throws Exception {
        mvc.perform(get("/api/reports/activity")).andExpect(status().isUnauthorized());
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
