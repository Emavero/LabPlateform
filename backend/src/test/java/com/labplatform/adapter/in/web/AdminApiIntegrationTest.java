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
                        {"title":"Analyser un journal Windows","topic":"LOG_ANALYSIS","level":"EASY",
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
                                {"title":"Analyser un journal Windows","topic":"LOG_ANALYSIS","level":"MEDIUM",
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
    void aQuizIsGradedByTheServerAndNeverRevealsItsAnswersBeforehand() throws Exception {
        Cookie admin = register("admin@example.com");
        Cookie learner = register("candidat@example.com");

        MvcResult created = mvc.perform(createCourse(admin, """
                        {"title":"Quiz de collecte","topic":"LOG_ANALYSIS","level":"EASY","summary":"",
                         "sections":[{"title":"Évaluation","kind":"QUIZ","minutes":10,"content":"Répondez.",
                           "questions":[
                             {"statement":"Premier geste ?","choices":[
                               {"label":"Capturer la mémoire","correct":true},
                               {"label":"Débrancher","correct":false}]},
                             {"statement":"On travaille sur ?","choices":[
                               {"label":"Une copie","correct":true},
                               {"label":"L'original","correct":false}]}]}]}"""))
                .andExpect(status().isCreated())
                // L'administrateur voit les bonnes réponses : c'est lui qui les écrit.
                .andExpect(jsonPath("$.sections[0].questions.length()").value(2))
                .andExpect(jsonPath("$.sections[0].questions[0].choices[0].correct").value(true))
                .andReturn();
        String body = created.getResponse().getContentAsString();
        Number questionOne = JsonPath.read(body, "$.sections[0].questions[0].id");
        Number rightOne = JsonPath.read(body, "$.sections[0].questions[0].choices[0].id");
        Number wrongOne = JsonPath.read(body, "$.sections[0].questions[0].choices[1].id");
        Number questionTwo = JsonPath.read(body, "$.sections[0].questions[1].id");
        Number rightTwo = JsonPath.read(body, "$.sections[0].questions[1].choices[0].id");

        // L'apprenant reçoit les énoncés sans les réponses.
        mvc.perform(get("/api/courses/{slug}", "quiz-de-collecte").cookie(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections[0].questions[0].choices[0].correct").doesNotExist());

        // Copie ratée : rien n'est validé, mais la correction arrive.
        mvc.perform(gradeQuiz(learner, "quiz-de-collecte", "evaluation", """
                        {"%d":[%d],"%d":[%d]}""".formatted(questionOne.longValue(), wrongOne.longValue(),
                        questionTwo.longValue(), rightTwo.longValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(1))
                .andExpect(jsonPath("$.passed").value(false))
                .andExpect(jsonPath("$.answers[0].correctChoiceIds[0]").value(rightOne.intValue()));

        mvc.perform(get("/api/courses/{slug}", "quiz-de-collecte").cookie(learner))
                .andExpect(jsonPath("$.sections[0].completed").value(false));

        // Copie réussie : la section est validée sans avoir à la cocher.
        mvc.perform(gradeQuiz(learner, "quiz-de-collecte", "evaluation", """
                        {"%d":[%d],"%d":[%d]}""".formatted(questionOne.longValue(), rightOne.longValue(),
                        questionTwo.longValue(), rightTwo.longValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(true));

        mvc.perform(get("/api/courses/{slug}", "quiz-de-collecte").cookie(learner))
                .andExpect(jsonPath("$.sections[0].completed").value(true));
    }

    @Test
    void aLearnerIsRefusedEverywhereUnderApiAdmin() throws Exception {
        Cookie learner = register("apprenant2@example.com");

        mvc.perform(get("/api/admin/overview").cookie(learner)).andExpect(status().isForbidden());
        mvc.perform(createCourse(learner, """
                {"title":"Cours pirate","topic":"HARDENING","level":"EASY","summary":"",
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
                {"title":"Sans section","topic":"HARDENING","level":"EASY","summary":"","sections":[]}"""))
                .andExpect(status().isBadRequest());

        mvc.perform(createCourse(admin, """
                {"title":"Vidéo piégée","topic":"HARDENING","level":"EASY","summary":"",
                 "sections":[{"title":"S","kind":"THEORY","minutes":5,"content":"x",
                              "videoUrl":"javascript:alert(1)"}]}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminRoutesRequireASession() throws Exception {
        mvc.perform(get("/api/admin/overview")).andExpect(status().isUnauthorized());
    }

    private static RequestBuilder gradeQuiz(Cookie session, String slug, String section, String answers) {
        return post("/api/courses/{slug}/sections/{section}/quiz", slug, section).cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(answers);
    }

    @Test
    void aCourseKeepsItsAttackPathRealCaseAndDesignersThroughTheDatabase() throws Exception {
        Cookie admin = register("admin@example.com");
        Cookie learner = register("apprenant4@example.com");

        MvcResult created = mvc.perform(createCourse(admin, """
                        {"title":"Chaîne complète","topic":"MEMORY_ANALYSIS","level":"MEDIUM","summary":"Résumé.",
                         "sections":[{"title":"Introduction","kind":"THEORY","minutes":10,"content":"Du texte."}],
                         "briefing":{
                           "attackSummary":"De la pièce jointe à l'exfiltration.",
                           "stages":[
                             {"name":"Hameçonnage","description":"Une pièce jointe.","technique":"T1566.001"},
                             {"name":"Persistance","description":"Une tâche planifiée."},
                             {"name":"","description":""}],
                           "realCase":{"sector":"Cabinet comptable","situation":"Un poste lent un vendredi.",
                                       "stake":"300 dossiers clients.","outcome":"Onze notifications au lieu de 300."},
                           "designers":[
                             {"name":"Awa Diallo","role":"Analyste forensique"},
                             {"name":"Marc Lefèvre","role":"Expert judiciaire",
                              "avatarUrl":"https://exemple.test/marc.png"}]}}"""))
                .andExpect(status().isCreated())
                .andReturn();
        Number sectionId = JsonPath.read(created.getResponse().getContentAsString(), "$.sections[0].id");

        // L'apprenant lit le même dossier : rien n'est réservé à l'administration ici.
        mvc.perform(get("/api/courses/{slug}", "chaine-complete").cookie(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topic").value("MEMORY_ANALYSIS"))
                .andExpect(jsonPath("$.topicSlug").value("analyse-memoire"))
                .andExpect(jsonPath("$.track").value("FORENSICS"))
                .andExpect(jsonPath("$.attackPath.summary").value("De la pièce jointe à l'exfiltration."))
                // La ligne laissée vide est écartée, et les positions sont renumérotées.
                .andExpect(jsonPath("$.attackPath.stages.length()").value(2))
                .andExpect(jsonPath("$.attackPath.stages[0].position").value(1))
                .andExpect(jsonPath("$.attackPath.stages[0].technique").value("T1566.001"))
                .andExpect(jsonPath("$.attackPath.stages[1].position").value(2))
                .andExpect(jsonPath("$.attackPath.stages[1].technique").doesNotExist())
                .andExpect(jsonPath("$.realCase.sector").value("Cabinet comptable"))
                .andExpect(jsonPath("$.realCase.outcome").value("Onze notifications au lieu de 300."))
                .andExpect(jsonPath("$.designers.length()").value(2))
                .andExpect(jsonPath("$.designers[0].name").value("Awa Diallo"))
                // Sans avatar, la page présente le concepteur par ses initiales.
                .andExpect(jsonPath("$.designers[0].avatarUrl").doesNotExist())
                .andExpect(jsonPath("$.designers[0].initials").value("AD"))
                .andExpect(jsonPath("$.designers[1].avatarUrl").value("https://exemple.test/marc.png"));

        // Une refonte qui retire le dossier le retire vraiment : pas de rémanence.
        mvc.perform(put("/api/admin/courses/{slug}", "chaine-complete").cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Chaîne complète","topic":"MEMORY_ANALYSIS","level":"MEDIUM",
                                 "summary":"Résumé.",
                                 "sections":[{"id":%s,"title":"Introduction","kind":"THEORY","minutes":10,
                                              "content":"Du texte."}]}""".formatted(sectionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attackPath").doesNotExist())
                .andExpect(jsonPath("$.realCase").doesNotExist())
                .andExpect(jsonPath("$.designers.length()").value(0));
    }

    @Test
    void aSubDomainThatDoesNotExistIsRefused() throws Exception {
        Cookie admin = register("admin@example.com");

        mvc.perform(createCourse(admin, """
                        {"title":"Sous-domaine inventé","topic":"STEGANOGRAPHIE","level":"EASY","summary":"",
                         "sections":[{"title":"Section","kind":"THEORY","minutes":5,"content":"Texte."}]}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anAttackPathSummaryWithoutAnyStageIsRefused() throws Exception {
        Cookie admin = register("admin@example.com");

        // Le domaine refuse un résumé qui n'enchaîne rien ; l'API doit le dire.
        mvc.perform(createCourse(admin, """
                        {"title":"Chaîne vide","topic":"HARDENING","level":"EASY","summary":"",
                         "sections":[{"title":"Section","kind":"THEORY","minutes":5,"content":"Texte."}],
                         "briefing":{"attackSummary":"Une chaîne annoncée mais pas décrite.","stages":[]}}"""))
                .andExpect(status().isBadRequest());
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
