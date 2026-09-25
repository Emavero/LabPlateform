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
import org.springframework.test.web.servlet.RequestBuilder;

import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Parcours « compromettre une machine » de bout en bout : catalogue,
 * soumission de flags, progression et classement, à travers les adaptateurs
 * réels (HTTP, sécurité, JPA sur H2 avec le vrai schéma SQL).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BoxApiIntegrationTest {

    private static final String COOKIE = "LAB_SESSION";
    private static final String SLUG = "integration";
    private static final String USER_FLAG = "0f0e0d0c0b0a09080706050403020100";
    private static final String ROOT_FLAG = "1f1e1d1c1b1a19181716151413121110";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private BoxRepositoryPort boxes;

    @BeforeEach
    void seedKnownBox() {
        if (boxes.findBySlug(SLUG).isEmpty()) {
            boxes.save(Box.create(SLUG, "Integration", OperatingSystem.LINUX, Difficulty.EASY,
                    "Machine de test du parcours complet.", "10.10.10.99", "cyberMans",
                    Instant.parse("2026-01-01T00:00:00Z"), Flag.ofSecret(USER_FLAG), Flag.ofSecret(ROOT_FLAG)));
        }
    }

    @Test
    void theCatalogueDescribesAMachineWithoutEverExposingItsFlags() throws Exception {
        Cookie session = register("catalogue@example.com");

        mvc.perform(get("/api/boxes/{slug}", SLUG).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Integration"))
                .andExpect(jsonPath("$.difficulty").value("EASY"))
                .andExpect(jsonPath("$.userFlagPoints").value(8))
                .andExpect(jsonPath("$.rootFlagPoints").value(12))
                .andExpect(jsonPath("$.totalPoints").value(20))
                .andExpect(jsonPath("$.ipAddress").value("10.10.10.99"))
                .andExpect(jsonPath("$.userOwned").value(false))
                .andExpect(jsonPath("$.pwned").value(false))
                // Ni le flag, ni son empreinte, ni le champ qui les porterait.
                .andExpect(content().string(not(containsString(USER_FLAG))))
                .andExpect(content().string(not(containsString(Flag.ofSecret(USER_FLAG).hash()))))
                .andExpect(content().string(not(containsString("FlagHash"))));

        mvc.perform(get("/api/boxes").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug == '" + SLUG + "')].totalPoints").value(20));
    }

    @Test
    void submittingBothFlagsPwnsTheMachineAndFeedsTheScoreboard() throws Exception {
        Cookie session = register("pwner@example.com");

        mvc.perform(submit(session, "USER", "deadbeefdeadbeefdeadbeefdeadbeef"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Flag incorrect"));

        mvc.perform(submit(session, "USER", USER_FLAG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("USER"))
                .andExpect(jsonPath("$.pointsAwarded").value(8))
                .andExpect(jsonPath("$.pwned").value(false))
                .andExpect(jsonPath("$.progress.points").value(8))
                .andExpect(jsonPath("$.progress.rankName").exists());

        mvc.perform(submit(session, "USER", USER_FLAG))
                .andExpect(status().isConflict());

        mvc.perform(submit(session, "ROOT", ROOT_FLAG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsAwarded").value(12))
                .andExpect(jsonPath("$.pwned").value(true))
                .andExpect(jsonPath("$.progress.points").value(20))
                .andExpect(jsonPath("$.progress.boxesPwned").value(1));

        mvc.perform(get("/api/scoreboard/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").value(20))
                .andExpect(jsonPath("$.ownedFlags").value(2))
                .andExpect(jsonPath("$.boxesPwned").value(1));

        mvc.perform(get("/api/scoreboard").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[?(@.self == true)].points").value(20))
                .andExpect(jsonPath("$.entries[?(@.self == true)].handle").value("pwner"))
                .andExpect(content().string(not(containsString("@example.com"))));
    }

    @Test
    void aMalformedSubmissionOrAnUnknownMachineIsRefused() throws Exception {
        Cookie session = register("malformed@example.com");

        mvc.perform(submit(session, "USER", "pas-un-flag"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/boxes/{slug}/flags", "machine-inconnue").cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"USER\",\"flag\":\"" + USER_FLAG + "\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/boxes/{slug}/flags", SLUG).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"ADMIN\",\"flag\":\"" + USER_FLAG + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theCatalogueAndTheScoreboardRequireASession() throws Exception {
        mvc.perform(get("/api/boxes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/scoreboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/scoreboard/me")).andExpect(status().isUnauthorized());
    }

    private static RequestBuilder submit(Cookie session, String kind, String flag) {
        return post("/api/boxes/{slug}/flags", SLUG).cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"" + kind + "\",\"flag\":\"" + flag + "\"}");
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
