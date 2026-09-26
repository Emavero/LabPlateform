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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Parcours des cours à travers les adaptateurs réels : les deux filières, le
 * suivi de lecture, et ce que le profil en déduit.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AcademyApiIntegrationTest {

    private static final String COOKIE = "LAB_SESSION";

    @Autowired
    private MockMvc mvc;

    @Test
    void theTwoTracksAreServedWithTheirCourses() throws Exception {
        Cookie session = register("etudiant@example.com");

        mvc.perform(get("/api/courses/tracks").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].slug").value("forensique"))
                .andExpect(jsonPath("$[0].name").value("Forensique"))
                .andExpect(jsonPath("$[1].slug").value("defense"))
                .andExpect(jsonPath("$[1].name").value("Défense"));

        mvc.perform(get("/api/courses").param("track", "forensique").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].track", org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is("FORENSICS"))))
                // Les plus accessibles d'abord.
                .andExpect(jsonPath("$[0].level").value("FUNDAMENTAL"));

        mvc.perform(get("/api/courses").param("track", "defense").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mvc.perform(get("/api/courses").param("track", "crypto").cookie(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void readingACourseSectionBySectionAdvancesTheProgress() throws Exception {
        Cookie session = register("lecteur@example.com");

        MvcResult detail = mvc.perform(get("/api/courses/{slug}", "bases-de-l-investigation").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackSlug").value("forensique"))
                .andExpect(jsonPath("$.sections.length()").value(4))
                .andExpect(jsonPath("$.sections[0].completed").value(false))
                .andExpect(jsonPath("$.sections[0].content").isNotEmpty())
                .andExpect(jsonPath("$.completed").value(false))
                .andReturn();
        String firstSection = JsonPath.read(detail.getResponse().getContentAsString(), "$.sections[0].slug");

        mvc.perform(post("/api/courses/{slug}/sections/{section}/completion", "bases-de-l-investigation", firstSection)
                        .cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sectionsCompleted").value(1))
                .andExpect(jsonPath("$.sections[0].completed").value(true));

        // Cocher deux fois ne compte qu'une fois.
        mvc.perform(post("/api/courses/{slug}/sections/{section}/completion", "bases-de-l-investigation", firstSection)
                        .cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sectionsCompleted").value(1));

        mvc.perform(get("/api/courses/progress").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trackSlug").value("forensique"))
                .andExpect(jsonPath("$[0].sectionsCompleted").value(1))
                .andExpect(jsonPath("$[1].sectionsCompleted").value(0));

        mvc.perform(get("/api/profile/activity").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kind").value("SECTION"))
                .andExpect(jsonPath("$[0].points").value(0));

        mvc.perform(get("/api/profile/achievements").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'STUDENT')].earned").value(true))
                .andExpect(jsonPath("$[?(@.code == 'GRADUATE')].earned").value(false));

        mvc.perform(delete("/api/courses/{slug}/sections/{section}/completion", "bases-de-l-investigation",
                        firstSection).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sectionsCompleted").value(0));
    }

    @Test
    void anUnknownCourseOrSectionIsNotFound() throws Exception {
        Cookie session = register("perdu@example.com");

        mvc.perform(get("/api/courses/{slug}", "cours-inexistant").cookie(session))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/courses/{slug}/sections/{section}/completion", "bases-de-l-investigation",
                        "section-inexistante").cookie(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void coursesAndProfileRequireASession() throws Exception {
        mvc.perform(get("/api/courses")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/courses/tracks")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/profile/achievements")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/profile/activity")).andExpect(status().isUnauthorized());
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
