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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Parcours « je m'abonne pour attaquer » de bout en bout, à travers les
 * adaptateurs réels. Le prestataire est l'encaisseur simulé, celui que la
 * configuration par défaut installe : le parcours HTTP est donc le vrai, seul
 * l'appel sortant est remplacé.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BillingApiIntegrationTest {

    private static final String COOKIE = "LAB_SESSION";
    private static final String FREE_SLUG = "vitrine-libre";
    private static final String PRO_SLUG = "vitrine-pro";
    private static final String USER_FLAG = "2f2e2d2c2b2a29282726252423222120";
    private static final String ROOT_FLAG = "3f3e3d3c3b3a39383736353433323130";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private BoxRepositoryPort boxes;

    @BeforeEach
    void seedBoxes() {
        if (boxes.findBySlug(FREE_SLUG).isEmpty()) {
            boxes.save(Box.create(FREE_SLUG, "Vitrine Libre", OperatingSystem.LINUX, Difficulty.VERY_EASY,
                    "Ouverte à tous.", "10.10.20.10", "cyberMans", Instant.parse("2026-01-01T00:00:00Z"),
                    false, false, Flag.ofSecret(USER_FLAG), Flag.ofSecret(ROOT_FLAG)));
        }
        if (boxes.findBySlug(PRO_SLUG).isEmpty()) {
            boxes.save(Box.create(PRO_SLUG, "Vitrine Pro", OperatingSystem.LINUX, Difficulty.MEDIUM,
                    "Réservée aux abonnés.", "10.10.20.11", "cyberMans", Instant.parse("2026-01-02T00:00:00Z"),
                    false, true, Flag.ofSecret(USER_FLAG), Flag.ofSecret(ROOT_FLAG)));
        }
    }

    /**
     * Le cœur de la règle : la fiche se voit — c'est ce qui donne envie — mais
     * ce qui sert à attaquer (adresse, synopsis) ne sort pas.
     */
    @Test
    void aFreeAccountSeesTheReservedMachineWithoutItsDetails() throws Exception {
        Cookie session = register("vitrine@example.com");

        mvc.perform(get("/api/boxes/{slug}", PRO_SLUG).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Vitrine Pro"))
                .andExpect(jsonPath("$.difficulty").value("MEDIUM"))
                .andExpect(jsonPath("$.totalPoints").value(30))
                .andExpect(jsonPath("$.locked").value(true))
                .andExpect(jsonPath("$.proOnly").value(true))
                .andExpect(jsonPath("$.ipAddress").doesNotExist())
                .andExpect(jsonPath("$.synopsis").value(""))
                .andExpect(content().string(not(containsString("10.10.20.11"))));

        // La machine ouverte, elle, se montre entièrement.
        mvc.perform(get("/api/boxes/{slug}", FREE_SLUG).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(false))
                .andExpect(jsonPath("$.ipAddress").value("10.10.20.10"));
    }

    /** Verrouillé veut dire verrouillé : pas seulement masqué à l'affichage. */
    @Test
    void aFreeAccountCannotActOnAReservedMachine() throws Exception {
        Cookie session = register("bloque@example.com");

        mvc.perform(post("/api/boxes/{slug}/flags", PRO_SLUG).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"USER\",\"flag\":\"" + USER_FLAG + "\"}"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.message").value("Cette machine est réservée aux abonnés Pro"));

        mvc.perform(post("/api/boxes/{slug}/instance", PRO_SLUG).cookie(session))
                .andExpect(status().isPaymentRequired());

        // Sur la machine ouverte, la même soumission est acceptée.
        mvc.perform(post("/api/boxes/{slug}/flags", FREE_SLUG).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"USER\",\"flag\":\"" + USER_FLAG + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void subscribingUnlocksTheReservedMachine() throws Exception {
        Cookie session = register("abonne@example.com");

        mvc.perform(get("/api/billing").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("FREE"))
                .andExpect(jsonPath("$.pro").value(false))
                .andExpect(jsonPath("$.offers.length()").value(4));

        String reference = checkout(session);

        mvc.perform(post("/api/billing/confirm").param("reference", reference).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("PRO"))
                .andExpect(jsonPath("$.pro").value(true))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.payments[0].status").value("SUCCEEDED"));

        mvc.perform(get("/api/boxes/{slug}", PRO_SLUG).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(false))
                .andExpect(jsonPath("$.ipAddress").value("10.10.20.11"))
                .andExpect(jsonPath("$.synopsis").value("Réservée aux abonnés."));

        mvc.perform(post("/api/boxes/{slug}/flags", PRO_SLUG).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"USER\",\"flag\":\"" + USER_FLAG + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsAwarded").value(12));
    }

    /** Le compte garde ce qu'il a payé, et cesse seulement de renouveler. */
    @Test
    void cancellingKeepsTheAccessUntilTheTerm() throws Exception {
        Cookie session = register("resilie@example.com");
        String reference = checkout(session);
        mvc.perform(post("/api/billing/confirm").param("reference", reference).cookie(session))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/billing").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.renewing").value(false))
                .andExpect(jsonPath("$.pro").value(true));

        mvc.perform(get("/api/boxes/{slug}", PRO_SLUG).cookie(session))
                .andExpect(jsonPath("$.locked").value(false));
    }

    /** Une référence qui n'est pas la sienne est introuvable, pas interdite. */
    @Test
    void anotherAccountsPaymentCannotBeConfirmed() throws Exception {
        Cookie mine = register("proprietaire@example.com");
        Cookie other = register("curieux-abo@example.com");
        String reference = checkout(mine);

        mvc.perform(post("/api/billing/confirm").param("reference", reference).cookie(other))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/billing").cookie(other)).andExpect(jsonPath("$.plan").value("FREE"));
    }

    @Test
    void theSubscriptionPagesRequireASession() throws Exception {
        mvc.perform(get("/api/billing")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/billing/checkout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"WAVE\",\"period\":\"MONTHLY\"}"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * La notification n'a pas de session — un serveur de paiement n'a pas de
     * cookie — mais un corps non signé ne doit rien accorder. L'encaisseur
     * simulé n'en lit aucune : la requête est donc acceptée sans rien changer.
     */
    @Test
    void anUnsignedNotificationChangesNothing() throws Exception {
        Cookie session = register("webhook@example.com");
        checkout(session);

        mvc.perform(post("/api/billing/webhooks/{method}", "WAVE")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SUCCEEDED\"}"))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/billing").cookie(session)).andExpect(jsonPath("$.plan").value("FREE"));
    }

    private String checkout(Cookie session) throws Exception {
        MvcResult result = mvc.perform(post("/api/billing/checkout").cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"WAVE\",\"period\":\"MONTHLY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirectUrl").exists())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        String reference = body.replaceAll(".*\"reference\"\\s*:\\s*\"([0-9a-f]{32})\".*", "$1");
        assertEquals(32, reference.length(), "la référence doit être un jeton de 32 caractères hexadécimaux");
        return reference;
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
