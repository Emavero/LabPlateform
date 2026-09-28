package com.labplatform.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Santé de la plateforme, serveur d'envoi injoignable.
 * <p>
 * C'est la sonde que Docker interroge : si elle répond DOWN, le conteneur est
 * déclaré malade et le frontend refuse de démarrer. Elle ne doit donc dépendre
 * que de ce sans quoi la plateforme ne sert à rien — la base de données — et
 * pas d'une fonction accessoire comme l'envoi de courriels.
 * <p>
 * Ce test existe parce que le contraire s'est produit : le seul ajout du
 * starter mail a suffi à faire répondre DOWN à toute la plateforme dès que
 * l'hôte SMTP n'existait pas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.mail.host=serveur-smtp-qui-nexiste-pas.invalid",
        "spring.mail.port=1025",
})
class HealthApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void laPlateformeResteSaineQuandLeServeurDEnvoiEstInjoignable() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
