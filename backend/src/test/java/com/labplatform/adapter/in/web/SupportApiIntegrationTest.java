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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Parcours d'assistance de bout en bout, à travers les adaptateurs réels.
 * <p>
 * Il vérifie surtout ce que les tests en mémoire ne peuvent pas voir :
 * l'enregistrement d'un fil déjà écrit, où seuls les messages neufs doivent
 * partir en base, et le cloisonnement d'une demande entre deux comptes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SupportApiIntegrationTest {

    private static final String COOKIE = "LAB_SESSION";

    @Autowired
    private MockMvc mvc;

    @Test
    void unEchangeCompletTientDansUnSeulFil() throws Exception {
        Cookie asker = register("demandeur-support@example.com");
        Cookie staff = register("admin@example.com");

        String created = mvc.perform(post("/api/support/tickets").cookie(asker)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"BILLING\",\"subject\":\"Paiement Wave refusé\","
                                + "\"body\":\"Le paiement échoue à l'étape 2.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.messages.length()").value(1))
                .andExpect(jsonPath("$.mine").value(true))
                .andReturn().getResponse().getContentAsString();
        long id = id(created);

        // Réponse de l'équipe : le fil s'allonge, il ne se réécrit pas.
        mvc.perform(post("/api/support/tickets/" + id + "/messages").cookie(staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Quel opérateur mobile ?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ANSWERED"))
                .andExpect(jsonPath("$.messages.length()").value(2))
                .andExpect(jsonPath("$.messages[1].fromStaff").value(true));

        // Le demandeur relance : la demande revient dans la file de l'équipe.
        mvc.perform(post("/api/support/tickets/" + id + "/messages").cookie(asker)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Orange.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.messages.length()").value(3))
                .andExpect(jsonPath("$.messages[2].fromStaff").value(false));

        mvc.perform(post("/api/support/tickets/" + id + "/resolution").cookie(asker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.messages.length()").value(3));

        // Relu depuis la base : les trois messages y sont, dans l'ordre.
        mvc.perform(get("/api/support/tickets/" + id).cookie(asker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(3))
                .andExpect(jsonPath("$.messages[0].body").value(containsString("étape 2")))
                .andExpect(jsonPath("$.messages[2].body").value("Orange."));
    }

    @Test
    void laDemandeDUnAutreCompteEstIntrouvable() throws Exception {
        Cookie mine = register("cloison-mienne@example.com");
        Cookie other = register("cloison-autre@example.com");

        long id = id(mvc.perform(post("/api/support/tickets").cookie(mine)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"LAB\",\"subject\":\"VPN coupé\",\"body\":\"Plus de route.\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        mvc.perform(get("/api/support/tickets/" + id).cookie(other)).andExpect(status().isNotFound());
        mvc.perform(get("/api/support/tickets").cookie(other))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void laFileEstRefuseeAUnCompteOrdinaire() throws Exception {
        Cookie player = register("file-refusee@example.com");

        mvc.perform(get("/api/admin/support/queue").cookie(player)).andExpect(status().isForbidden());
    }

    @Test
    void laFileMontreLesDemandesEnAttenteALAdministration() throws Exception {
        Cookie asker = register("file-attente@example.com");
        Cookie staff = register("admin@example.com");
        mvc.perform(post("/api/support/tickets").cookie(asker).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"MACHINES\",\"subject\":\"Cible injoignable\","
                                + "\"body\":\"Aucune réponse au ping.\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/admin/support/queue").cookie(staff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waiting[0].subject").value("Cible injoignable"))
                .andExpect(jsonPath("$.waiting[0].handle").value("file-attente"))
                .andExpect(jsonPath("$.waiting[0].lastMessage").value(containsString("ping")))
                .andExpect(jsonPath("$.byCategory[0].category").value("MACHINES"));
    }

    @Test
    void uneDemandeSansSujetEstRefusee() throws Exception {
        Cookie asker = register("sujet-vide@example.com");

        mvc.perform(post("/api/support/tickets").cookie(asker).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"OTHER\",\"subject\":\"  \",\"body\":\"Un message.\"}"))
                .andExpect(status().isBadRequest());
    }

    private static long id(String json) {
        int start = json.indexOf("\"id\":") + 5;
        int end = json.indexOf(',', start);
        return Long.parseLong(json.substring(start, end).trim());
    }

    private Cookie register(String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\","
                                + "\"confirmPassword\":\"password123\"}"))
                .andReturn();
        // Le compte d'administration peut déjà exister d'un test précédent :
        // dans ce cas, on ouvre simplement une session.
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
