package com.labplatform.domain.scenario;

import com.labplatform.domain.box.FlagKind;
import com.labplatform.domain.shared.ConflictException;
import com.labplatform.domain.shared.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioTest {

    private static final Instant NOW = Instant.parse("2026-09-27T10:00:00Z");

    private static ScenarioStep machine(int position, String slug, ScenarioObjective objective) {
        return new ScenarioStep((long) position, position, ScenarioStepKind.MACHINE, slug, "Consigne.", objective);
    }

    private static ScenarioStep course(int position, String slug) {
        return new ScenarioStep((long) position, position, ScenarioStepKind.COURSE, slug, null, null);
    }

    private static Scenario scenario(List<ScenarioStep> steps) {
        return Scenario.restore(1L, "intrusion-guidee", "Intrusion guidée", "Mise en situation.", true, NOW, NOW,
                steps);
    }

    @Test
    void leLienEstDeriveDuTitreUneFoisPourToutes() {
        Scenario composed = Scenario.compose("Réponse à incident", "Situation.",
                List.of(machine(1, "sentinel", ScenarioObjective.USER_FLAG)), true, NOW);

        assertEquals("reponse-a-incident", composed.getSlug());

        composed.revise("Autre titre", "Situation.", List.of(machine(1, "sentinel", ScenarioObjective.USER_FLAG)),
                true, NOW);

        assertEquals("reponse-a-incident", composed.getSlug());
        assertEquals("Autre titre", composed.getTitle());
    }

    @Test
    void lesEtapesSeSuiventDansLOrdreDeLeurPosition() {
        Scenario scenario = scenario(List.of(machine(3, "c", ScenarioObjective.USER_FLAG),
                machine(1, "a", ScenarioObjective.USER_FLAG), machine(2, "b", ScenarioObjective.USER_FLAG)));

        assertEquals(List.of("a", "b", "c"), scenario.getSteps().stream().map(ScenarioStep::reference).toList());
    }

    /** Un scénario publié sans étape compterait pour terminé dès son ouverture. */
    @Test
    void unScenarioPublieSansEtapeEstRefuse() {
        assertThrows(ConflictException.class,
                () -> Scenario.compose("Vide", "Situation.", List.of(), true, NOW));
    }

    @Test
    void unBrouillonPeutEtreVide() {
        Scenario draft = Scenario.compose("Brouillon", "Situation.", List.of(), false, NOW);

        assertEquals(0, draft.getStepCount());
        assertFalse(draft.isPublished());
    }

    @Test
    void uneMiseEnSituationVideEstRefusee() {
        assertThrows(InvalidInputException.class,
                () -> Scenario.compose("Titre", "   ", List.of(machine(1, "a", ScenarioObjective.USER_FLAG)), false,
                        NOW));
    }

    @Test
    void unObjectifEstImposeAUneEtapeDeMachineEtRetireAUnCours() {
        ScenarioStep sansObjectif = new ScenarioStep(1L, 1, ScenarioStepKind.MACHINE, "sentinel", null, null);
        ScenarioStep cours = new ScenarioStep(2L, 2, ScenarioStepKind.COURSE, "triage", null,
                ScenarioObjective.ROOT_FLAG);

        assertEquals(ScenarioObjective.USER_FLAG, sansObjectif.objective());
        assertNull(cours.objective());
    }

    @Test
    void uneEtapeSansReferenceEstRefusee() {
        assertThrows(InvalidInputException.class,
                () -> new ScenarioStep(1L, 1, ScenarioStepKind.MACHINE, "  ", null, null));
    }

    @Test
    void lAvancementSuitLesFlagsValidesEtLesCoursTermines() {
        Scenario scenario = scenario(List.of(machine(1, "sentinel", ScenarioObjective.USER_FLAG),
                machine(2, "citadelle", ScenarioObjective.BOTH_FLAGS), course(3, "triage")));
        ScenarioEvidence evidence = new ScenarioEvidence(
                Map.of("sentinel", Set.of(FlagKind.USER), "citadelle", Set.of(FlagKind.USER)),
                Set.of("triage"));

        ScenarioProgress progress = scenario.progressOf(evidence);

        assertEquals(2, progress.done());
        assertEquals(3, progress.total());
        assertFalse(progress.isComplete());
        // La première étape non franchie est celle à faire, même si la suivante l'est.
        assertEquals(2, progress.nextPosition());
    }

    @Test
    void unScenarioEntierementFranchiNAPlusDEtapeSuivante() {
        Scenario scenario = scenario(List.of(machine(1, "sentinel", ScenarioObjective.ROOT_FLAG)));
        ScenarioEvidence evidence = new ScenarioEvidence(Map.of("sentinel", Set.of(FlagKind.ROOT)), Set.of());

        ScenarioProgress progress = scenario.progressOf(evidence);

        assertTrue(progress.isComplete());
        assertNull(progress.nextPosition());
        assertEquals(1.0, progress.ratio());
    }

    @Test
    void lesDeuxFlagsSontExigesQuandLObjectifLeDemande() {
        Scenario scenario = scenario(List.of(machine(1, "sentinel", ScenarioObjective.BOTH_FLAGS)));

        assertFalse(scenario.progressOf(
                new ScenarioEvidence(Map.of("sentinel", Set.of(FlagKind.USER)), Set.of())).isComplete());
        assertTrue(scenario.progressOf(
                new ScenarioEvidence(Map.of("sentinel", Set.of(FlagKind.USER, FlagKind.ROOT)), Set.of()))
                .isComplete());
    }

    @Test
    void sansAucunePreuveRienNEstFranchi() {
        Scenario scenario = scenario(List.of(machine(1, "sentinel", ScenarioObjective.USER_FLAG), course(2, "triage")));

        ScenarioProgress progress = scenario.progressOf(ScenarioEvidence.NONE);

        assertEquals(0, progress.done());
        assertEquals(1, progress.nextPosition());
        assertEquals(0.0, progress.ratio());
    }

    /** Un scénario sans étape n'est pas « terminé » : il n'y a rien à franchir. */
    @Test
    void unScenarioSansEtapeNEstJamaisTermine() {
        Scenario draft = Scenario.compose("Brouillon", "Situation.", List.of(), false, NOW);

        ScenarioProgress progress = draft.progressOf(ScenarioEvidence.NONE);

        assertFalse(progress.isComplete());
        assertEquals(0.0, progress.ratio());
    }

    @Test
    void auDelaDeVingtEtapesLeScenarioEstRefuse() {
        List<ScenarioStep> trop = new java.util.ArrayList<>();
        for (int i = 1; i <= 21; i++) {
            trop.add(machine(i, "machine-" + i, ScenarioObjective.USER_FLAG));
        }

        assertThrows(InvalidInputException.class, () -> Scenario.compose("Trop", "Situation.", trop, true, NOW));
    }

    @Test
    void leFilDEtapesNEstPasModifiableDeLExterieur() {
        Scenario scenario = scenario(List.of(machine(1, "sentinel", ScenarioObjective.USER_FLAG)));

        assertThrows(UnsupportedOperationException.class,
                () -> scenario.getSteps().add(machine(2, "autre", ScenarioObjective.USER_FLAG)));
    }
}
