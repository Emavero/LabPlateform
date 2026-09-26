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
import org.springframework.test.web.servlet.RequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Administration des cours de bout en bout : qui a le droit, ce que la
 * publication produit côté apprenant, et ce qu'une refonte préserve.
 * <p>
 * Le compte administrateur vient de app.security.admin-emails (profil test).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminApiIntegrationTest {

    private static final String COOKIE = "LAB_SESSION";

    @Autowired
    private MockMvc mvc;

    @Test
    void anAdminPublishesACourseThatLearnersSeeImmediately() throws Exception {
        Cookie admin = register("admin@example.com");
        Cookie learner = register("apprenant1@example.com");

        MvcResult created = mvc.perform(createCourse(admin, """
                        {"title":"Analyser un journal Windows","track":"FORENSICS","level":"EASY",
                         "summary":"Lire les journaux d'événements.",
                         "sections":[
                           {"title":"Introduction","kind":"THEORY","minutes":10,"content":"Du texte.",
                            "videoUrl":"https://www.youtube.com/watch?v=dQw4w9WgXcQ"},
                           {"title":"Atelier","kind":"LAB","minutes":25,"content":"Des commandes."}]}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("analyser-un-journal-windows"))
                .andExpect(jsonPath("$.sections.length()").value(2))
                .andExpect(jsonPath("$.sections[0].videoUrl").value("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
                .andExpect(jsonPath("$.sections[1].videoUrl").doesNotExist())
                .andReturn();
        Number sectionId = JsonPath.read(created.getResponse().getContentAsString(), "$.sections[0].id");

        mvc.perform(get("/api/courses/{slug}", "analyser-un-journal-windows").cookie(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Analyser un journal Windows"))
                .andExpect(jsonPath("$.sections[0].content").value("Du texte."));

        // L'apprenant termine la première section…
        mvc.perform(post("/api/courses/{slug}/sections/{section}/completion", "analyser-un-journal-windows",
                        "introduction").cookie(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sectionsCompleted").value(1));

        // …que l'administrateur renomme : l'avancement survit, car l'identifiant est renvoyé.
        mvc.perform(put("/api/admin/courses/{slug}", "analyser-un-journal-windows").cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Analyser un journal Windows","track":"FORENSICS","level":"MEDIUM",
                                 "summary":"Version remaniée.",
                                 "sections":[{"id":%d,"title":"Prise en main","kind":"THEORY","minutes":12,
                                              "content":"Texte revu."}]}""".formatted(sectionId.longValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value("MEDIUM"))
                .andExpect(jsonPath("$.sections.length()").value(1))
                .andExpect(jsonPath("$.sections[0].title").value("Prise en main"));

        mvc.perform(get("/api/courses/{slug}", "analyser-un-journal-windows").cookie(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sectionsCompleted").value(1))
                .andExpect(jsonPath("$.completed").value(true));

        mvc.perform(delete("/api/admin/courses/{slug}", "analyser-un-journal-windows").cookie(admin))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/courses/{slug}", "analyser-un-journal-windows").cookie(learner))
                .andExpect(status().isNotFound());
    }

    @Test
    void aLearnerIsRefusedEverywhereUnderApiAdmin() throws Exception {
        Cookie learner = register("apprenant2@example.com");

        mvc.perform(get("/api/admin/overview").cookie(learner)).andExpect(status().isForbidden());
        mvc.perform(createCourse(learner, """
                {"title":"Cours pirate","track":"DEFENSE","level":"EASY","summary":"",
                 "sections":[{"title":"S","kind":"THEORY","minutes":5,"content":"x"}]}"""))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/courses/{slug}", "durcissement-des-systemes").cookie(learner))
                .andExpect(status().isForbidden());
    }

    @Test
    void theAdminOverviewCountsTheContentAndItsUse() throws Exception {
        Cookie admin = register("admin@example.com");

        mvc.perform(get("/api/admin/overview").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courses").isNumber())
                .andExpect(jsonPath("$.sections").isNumber())
                .andExpect(jsonPath("$.users").isNumber())
                .andExpect(jsonPath("$.boxes").isNumber());
    }

    @Test
    void aDraftWithoutSectionOrWithAHostileVideoIsRefused() throws Exception {
        Cookie admin = register("admin@example.com");

        mvc.perform(createCourse(admin, """
                {"title":"Sans section","track":"DEFENSE","level":"EASY","summary":"","sections":[]}"""))
                .andExpect(status().isBadRequest());

        mvc.perform(createCourse(admin, """
                {"title":"Vidéo piégée","track":"DEFENSE","level":"EASY","summary":"",
                 "sections":[{"title":"S","kind":"THEORY","minutes":5,"content":"x",
                              "videoUrl":"javascript:alert(1)"}]}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminRoutesRequireASession() throws Exception {
        mvc.perform(get("/api/admin/overview")).andExpect(status().isUnauthorized());
    }

    private static RequestBuilder createCourse(Cookie session, String body) {
        return post("/api/admin/courses").cookie(session).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    /** Réutilise la session si le compte existe déjà (les tests partagent la base). */
    private Cookie register(String email) throws Exception {
        String credentials = "{\"email\":\"" + email + "\",\"password\":\"password123\","
                + "\"confirmPassword\":\"password123\"}";
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
