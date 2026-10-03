package com.labplatform.application.service;

import com.labplatform.application.fakes.Fakes;
import com.labplatform.application.fakes.InMemoryBoxes;
import com.labplatform.application.fakes.InMemoryCompletions;
import com.labplatform.application.fakes.InMemoryCourses;
import com.labplatform.application.fakes.InMemoryOwns;
import com.labplatform.application.fakes.InMemoryScenarios;
import com.labplatform.application.port.in.scenario.ScenarioDraft;
import com.labplatform.application.port.in.scenario.ScenarioView;
import com.labplatform.domain.academy.Course;
import com.labplatform.domain.academy.CourseLevel;
import com.labplatform.domain.academy.CourseTopic;
import com.labplatform.domain.academy.CourseSection;
import com.labplatform.domain.academy.SectionCompletion;
import com.labplatform.domain.academy.SectionKind;
import com.labplatform.domain.academy.Track;
import com.labplatform.domain.box.Box;
import com.labplatform.domain.box.Difficulty;
import com.labplatform.domain.box.Flag;
import com.labplatform.domain.box.FlagKind;
import com.labplatform.domain.box.Own;
import com.labplatform.domain.lab.OperatingSystem;
import com.labplatform.domain.scenario.ScenarioObjective;
import com.labplatform.domain.scenario.ScenarioStepKind;
import com.labplatform.domain.shared.ForbiddenException;
import com.labplatform.domain.shared.InvalidInputException;
import com.labplatform.domain.shared.NotFoundException;
import com.labplatform.domain.user.Actor;
import com.labplatform.domain.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-27T10:00:00Z");
    private static final String USER_FLAG = "8f8e8d8c8b8a89888786858483828180";
    private static final String ROOT_FLAG = "9f9e9d9c9b9a99989796959493929190";

    private final InMemoryScenarios scenarios = new InMemoryScenarios();
    private final InMemoryBoxes boxes = new InMemoryBoxes();
    private final InMemoryCourses courses = new InMemoryCourses();
    private final InMemoryOwns owns = new InMemoryOwns();
    private final InMemoryCompletions completions = new InMemoryCompletions();

    private final Actor alice = new Actor(1L, Role.USER);
    private final Actor admin = new Actor(2L, Role.ADMIN);
    private ScenarioService service;
    private Long openBoxId;
    private Long proBoxId;
    private Course course;

    @BeforeEach
    void setUp() {
        service = new ScenarioService(scenarios, boxes, courses, owns, completions, Fakes.FREE_PLAN,
                Fakes.NO_TRANSACTION, Clock.fixed(NOW, ZoneOffset.UTC));
        openBoxId = boxes.save(Box.create("porte", "Porte", OperatingSystem.LINUX, Difficulty.VERY_EASY, "Synopsis.",
                "10.10.10.10", "cyberMans", NOW, false, false, Flag.ofSecret(USER_FLAG),
                Flag.ofSecret(ROOT_FLAG))).getId();
        proBoxId = boxes.save(Box.create("citadelle", "Citadelle", OperatingSystem.LINUX, Difficulty.HARD,
                "Synopsis.", "10.10.10.20", "cyberMans", NOW, false, true, Flag.ofSecret(USER_FLAG),
                Flag.ofSecret(ROOT_FLAG))).getId();
        course = courses.save(Course.create("triage", "Triage mémoire", CourseTopic.MEMORY_ANALYSIS, CourseLevel.FUNDAMENTAL,
                "Résumé.", NOW, List.of(CourseSection.of(null, "intro", "Introduction", SectionKind.THEORY, 1, 20, "Texte."))));
    }

    private ScenarioDraft draft(boolean published, List<ScenarioDraft.StepDraft> steps) {
        return new ScenarioDraft("Intrusion guidée", "Mise en situation.", published, steps);
    }

    private static ScenarioDraft.StepDraft machine(String slug, ScenarioObjective objective) {
        return new ScenarioDraft.StepDraft(ScenarioStepKind.MACHINE, slug, "Consigne.", objective);
    }

    private static ScenarioDraft.StepDraft courseStep(String slug) {
        return new ScenarioDraft.StepDraft(ScenarioStepKind.COURSE, slug, null, null);
    }

    @Test
    void laConceptionEstReserveeALAdministration() {
        assertThrows(ForbiddenException.class,
                () -> service.create(alice, draft(true, List.of(machine("porte", ScenarioObjective.USER_FLAG)))));
        assertThrows(ForbiddenException.class, () -> service.listAll(alice));
    }

    @Test
    void laPositionDesEtapesVientDeLOrdreDeLaListe() {
        service.create(admin, draft(true, List.of(machine("porte", ScenarioObjective.USER_FLAG),
                courseStep("triage"), machine("citadelle", ScenarioObjective.BOTH_FLAGS))));

        ScenarioView view = service.get(alice, "intrusion-guidee");

        assertEquals(List.of(1, 2, 3), view.steps().stream().map(step -> step.step().position()).toList());
        assertEquals(List.of("porte", "triage", "citadelle"),
                view.steps().stream().map(step -> step.step().reference()).toList());
    }

    /**
     * Publier un scénario qui désigne une machine inexistante donnerait une étape
     * infranchissable, et le joueur en conclurait que la plateforme est cassée.
     */
    @Test
    void uneReferenceInconnueEstRefuseeALEnregistrement() {
        assertThrows(InvalidInputException.class,
                () -> service.create(admin, draft(true, List.of(machine("fantome", ScenarioObjective.USER_FLAG)))));
        assertThrows(InvalidInputException.class,
                () -> service.create(admin, draft(true, List.of(courseStep("cours-absent")))));
    }

    @Test
    void unBrouillonNEstPasListeAuxJoueurs() {
        service.create(admin, draft(false, List.of(machine("porte", ScenarioObjective.USER_FLAG))));

        assertTrue(service.listPublished(alice).isEmpty());
        assertEquals(1, service.listAll(admin).size());
        assertThrows(NotFoundException.class, () -> service.get(alice, "intrusion-guidee"));
    }

    @Test
    void lAvancementSeDeduitDesFlagsDejaValides() {
        service.create(admin, draft(true, List.of(machine("porte", ScenarioObjective.USER_FLAG),
                machine("citadelle", ScenarioObjective.BOTH_FLAGS))));
        owns.save(new Own(null, alice.userId(), openBoxId, FlagKind.USER, 4, false, NOW));

        ScenarioView view = service.get(alice, "intrusion-guidee");

        assertEquals(1, view.progress().done());
        assertEquals(2, view.progress().total());
        assertEquals(2, view.progress().nextPosition());
        assertTrue(view.steps().get(0).done());
        assertFalse(view.steps().get(1).done());
    }

    /** Une étape « suivre ce cours » ne se valide pas à la première page lue. */
    @Test
    void unCoursCompteTermineSeulementQuandToutesSesSectionsLeSont() {
        courses.save(Course.restore(course.getId(), course.getSlug(), course.getTitle(), course.getTopic(),
                course.getLevel(), course.getSummary(), course.getPublishedAt(),
                List.of(CourseSection.of(1L, "intro", "Introduction", SectionKind.THEORY, 1, 20, "Texte."),
                        CourseSection.of(2L, "suite", "Suite", SectionKind.THEORY, 2, 20, "Texte."))));
        service.create(admin, draft(true, List.of(courseStep("triage"))));
        completions.save(SectionCompletion.record(alice.userId(), course.getId(), 1L, NOW));

        assertFalse(service.get(alice, "intrusion-guidee").progress().isComplete());

        completions.save(SectionCompletion.record(alice.userId(), course.getId(), 2L, NOW));

        assertTrue(service.get(alice, "intrusion-guidee").progress().isComplete());
    }

    /** Une machine réservée reste visible dans le scénario, son détail non. */
    @Test
    void uneEtapeSurMachineReserveeEstSignaleeVerrouilleeSansAbonnement() {
        service.create(admin, draft(true, List.of(machine("citadelle", ScenarioObjective.USER_FLAG))));

        ScenarioView vueJoueur = service.get(alice, "intrusion-guidee");
        ScenarioView vueAdmin = service.get(admin, "intrusion-guidee");

        assertTrue(vueJoueur.steps().get(0).locked());
        assertEquals("Citadelle", vueJoueur.steps().get(0).name());
        assertFalse(vueAdmin.steps().get(0).locked());
    }

    /** Une ressource retirée après publication se signale : c'est à l'équipe de trancher. */
    @Test
    void uneRessourceRetireeApresPublicationEstSignaleeManquante() {
        service.create(admin, draft(true, List.of(machine("porte", ScenarioObjective.USER_FLAG))));
        boxes.findBySlug("porte").ifPresent(boxes::delete);

        ScenarioView view = service.get(admin, "intrusion-guidee");

        assertTrue(view.steps().get(0).missing());
        assertEquals("porte", view.steps().get(0).name());
    }

    @Test
    void laReecritureRemplaceLesEtapesEtGardeLeLien() {
        service.create(admin, draft(true, List.of(machine("porte", ScenarioObjective.USER_FLAG))));

        service.update(admin, "intrusion-guidee", draft(true, List.of(courseStep("triage"))));

        ScenarioView view = service.get(alice, "intrusion-guidee");
        assertEquals(1, view.steps().size());
        assertEquals("triage", view.steps().get(0).step().reference());
        assertEquals("Triage mémoire", view.steps().get(0).name());
    }

    @Test
    void laSuppressionRetireLeScenario() {
        service.create(admin, draft(true, List.of(machine("porte", ScenarioObjective.USER_FLAG))));

        service.delete(admin, "intrusion-guidee");

        assertTrue(service.listAll(admin).isEmpty());
        assertThrows(NotFoundException.class, () -> service.get(alice, "intrusion-guidee"));
    }

    @Test
    void leProgresAcquisAvantLOuvertureDuScenarioCompte() {
        owns.save(new Own(null, alice.userId(), proBoxId, FlagKind.USER, 16, false, NOW.minusSeconds(86_400)));
        service.create(admin, draft(true, List.of(machine("citadelle", ScenarioObjective.USER_FLAG))));

        assertTrue(service.get(alice, "intrusion-guidee").progress().isComplete());
    }
}
