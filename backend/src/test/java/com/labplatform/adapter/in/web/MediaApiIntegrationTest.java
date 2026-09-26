package com.labplatform.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Téléversement et lecture d'une vidéo de cours, à travers les adaptateurs
 * réels : multipart, stockage sur disque, service du fichier par plages.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MediaApiIntegrationTest {

    private static final String COOKIE = "LAB_SESSION";
    private static final byte[] VIDEO = "contenu-video-factice".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private MockMvc mvc;

    @Test
    void anAdminUploadsAVideoThatLearnersCanThenRead() throws Exception {
        Cookie admin = register("admin@example.com");
        Cookie learner = register("spectateur@example.com");

        MvcResult uploaded = mvc.perform(multipart("/api/admin/media")
                        .file(new MockMultipartFile("file", "intro.mp4", "video/mp4", VIDEO))
                        .cookie(admin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("video/mp4"))
                .andExpect(jsonPath("$.sizeBytes").value(VIDEO.length))
                .andExpect(jsonPath("$.filename").value("intro.mp4"))
                .andReturn();
        String url = JsonPath.read(uploaded.getResponse().getContentAsString(), "$.url");

        mvc.perform(get(url).cookie(learner))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "video/mp4"))
                .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));

        // Requête par plage : le navigateur doit pouvoir se déplacer dans la vidéo.
        mvc.perform(get(url).cookie(learner).header(HttpHeaders.RANGE, "bytes=0-4"))
                .andExpect(status().isPartialContent());
    }

    @Test
    void aLearnerCannotUploadAndAnAnonymousCannotRead() throws Exception {
        Cookie learner = register("simple@example.com");

        mvc.perform(multipart("/api/admin/media")
                        .file(new MockMultipartFile("file", "intro.mp4", "video/mp4", VIDEO))
                        .cookie(learner))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/media/{id}", "0".repeat(32))).andExpect(status().isUnauthorized());
    }

    @Test
    void aFormatThatIsNotAVideoIsRefused() throws Exception {
        Cookie admin = register("admin@example.com");

        mvc.perform(multipart("/api/admin/media")
                        .file(new MockMultipartFile("file", "page.html", "text/html",
                                "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8)))
                        .cookie(admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anUnknownFileIsNotFound() throws Exception {
        Cookie learner = register("curieux@example.com");

        mvc.perform(get("/api/media/{id}", "a".repeat(32)).cookie(learner)).andExpect(status().isNotFound());
        // Un identifiant qui n'a pas la bonne forme n'atteint jamais le stockage.
        mvc.perform(get("/api/media/{id}", "../../etc/passwd").cookie(learner))
                .andExpect(status().is4xxClientError());
    }

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
