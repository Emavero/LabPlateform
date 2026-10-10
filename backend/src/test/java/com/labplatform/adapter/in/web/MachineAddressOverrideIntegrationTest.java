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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * APP_MACHINE_ADDRESS depuis la variable d'environnement jusqu'au JSON rendu à
 * l'apprenant.
 * <p>
 * Les tests de l'adaptateur passent l'adresse au constructeur : ils prouvent la
 * substitution, pas le câblage. Or entre la variable et l'adaptateur il y a une
 * chaîne entière — le gabarit {@code ${APP_MACHINE_ADDRESS:}} d'application.yml,
 * la liaison relâchée de Spring sur {@code app.machine.address}, et la
 * composition dans GcpConfig. Une faute de frappe dans n'importe lequel de ces
 * maillons laisse passer toute la suite et ne se voit qu'en production, sous la
 * forme exacte du défaut qu'on voulait corriger : l'adresse du VPC affichée à
 * l'apprenant, et rien qui répond.
 * <p>
 * Le contexte est donc monté avec la variable telle que l'exploitant l'écrit.
 */
@SpringBootTest(properties = "APP_MACHINE_ADDRESS=10.8.0.1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MachineAddressOverrideIntegrationTest {

    private static final String COOKIE = "LAB_SESSION";

    @Autowired
    private MockMvc mvc;

    @Test
    void theAdvertisedAddressReachesTheLearnerInsteadOfTheProviderOne() throws Exception {
        Cookie session = register("machine-address@example.com");

        // La cible simulée annonce 10.10.10.10 ; l'exploitant annonce 10.8.0.1.
        // C'est la seconde qui doit sortir, sans quoi le tunnel ne mène nulle part.
        mvc.perform(post("/api/machine/start").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.internalIp").value("10.8.0.1"));

        // Éteinte, aucune adresse n'est annoncée : en poser une ferait croire
        // qu'on peut encore s'y connecter.
        mvc.perform(post("/api/machine/stop").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.internalIp").doesNotExist());
    }

    private Cookie register(String email) throws Exception {
        String credentials = """
                {"email":"%s","password":"password123","confirmPassword":"password123"}""".formatted(email);
        MvcResult result = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(credentials)).andExpect(status().isCreated()).andReturn();
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        return new Cookie(COOKIE, setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';')));
    }
}
