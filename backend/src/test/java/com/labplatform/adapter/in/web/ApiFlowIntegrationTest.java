package com.labplatform.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Parcours complet à travers tous les adaptateurs réels (HTTP, sécurité,
 * JPA sur H2 en mode PostgreSQL avec le vrai schéma SQL, hyperviseur simulé).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiFlowIntegrationTest {

    private static final String COOKIE = "LAB_SESSION";

    @Autowired
    private MockMvc mvc;

    @Test
    void registerThenOperateTheLab() throws Exception {
        Cookie session = register("flow@example.com");

        mvc.perform(get("/api/users/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("flow@example.com"));

        MvcResult list = mvc.perform(get("/api/labs/vms").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].os").value("WINDOWS"))
                .andExpect(jsonPath("$[1].os").value("LINUX"))
                .andExpect(jsonPath("$[1].status").value("STOPPED"))
                .andExpect(jsonPath("$[1].connection").doesNotExist())
                .andReturn();
        long linuxId = ((Number) JsonPath.read(list.getResponse().getContentAsString(), "$[1].id")).longValue();

        mvc.perform(post("/api/labs/vms/{id}/start", linuxId).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.connection.protocol").value("SSH"))
                .andExpect(jsonPath("$.connection.port").value(22))
                .andExpect(jsonPath("$.connection.username").value("labuser"));

        mvc.perform(post("/api/labs/vms/{id}/start", linuxId).cookie(session))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/labs/vms/{id}/stop", linuxId).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("STOPPED"))
                .andExpect(jsonPath("$.connection").doesNotExist());

        Cookie intruder = register("intruder@example.com");
        mvc.perform(get("/api/labs/vms/{id}", linuxId).cookie(intruder))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/labs/vms/{id}/start", linuxId).cookie(intruder))
                .andExpect(status().isNotFound());
    }

    @Test
    void protectedRoutesRequireASession() throws Exception {
        mvc.perform(get("/api/labs/vms")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/labs/vms").cookie(new Cookie(COOKIE, "forged.token.value")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRejectsWrongPasswordAndDuplicateEmailsAreRefused() throws Exception {
        register("login@example.com");

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"login@example.com\",\"password\":\"not-the-password\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"LOGIN@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registration("login@example.com")))
                .andExpect(status().isConflict());
    }

    @Test
    void logoutExpiresTheCookie() throws Exception {
        mvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }

    private Cookie register(String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registration(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Strict")))
                .andReturn();
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        String token = setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
        return new Cookie(COOKIE, token);
    }

    private static String registration(String email) {
        return "{\"email\":\"" + email + "\",\"password\":\"password123\",\"confirmPassword\":\"password123\"}";
    }
}
