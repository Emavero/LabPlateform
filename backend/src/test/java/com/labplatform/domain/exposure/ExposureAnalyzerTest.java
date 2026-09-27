package com.labplatform.domain.exposure;

import com.labplatform.domain.box.Difficulty;
import com.labplatform.domain.lab.OperatingSystem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExposureAnalyzerTest {

    private static TargetFacts target(String slug, OperatingSystem system, Difficulty difficulty, String address,
                                      boolean proOnly, long userOwns, long rootOwns, long attempts) {
        return new TargetFacts(slug, slug, system, difficulty, address, false, proOnly, userOwns, rootOwns, attempts,
                0);
    }

    @Test
    void uneCibleOuverteFacileEtLargementPossedeeEstCritique() {
        TargetFacts facile = target("porte", OperatingSystem.LINUX, Difficulty.VERY_EASY, "10.10.10.10", false,
                18, 16, 4);

        TargetExposure exposure = ExposureAnalyzer.analyse(List.of(facile), true).get(0);

        assertEquals(ExposureLevel.CRITICAL, exposure.level());
        assertTrue(exposure.signals().contains(ExposureSignal.OPEN_TO_ALL));
        assertTrue(exposure.signals().contains(ExposureSignal.LOW_DIFFICULTY));
        assertTrue(exposure.signals().contains(ExposureSignal.WIDELY_OWNED));
        assertTrue(exposure.signals().contains(ExposureSignal.FAST_ESCALATION));
    }

    @Test
    void uneCibleReserveeEtDifficileResteFaiblementExposee() {
        TargetFacts dure = target("citadelle", OperatingSystem.LINUX, Difficulty.INSANE, "10.10.30.10", true,
                1, 0, 40);

        TargetExposure exposure = ExposureAnalyzer.analyse(List.of(dure), true).get(0);

        assertEquals(ExposureLevel.LOW, exposure.level());
        assertFalse(exposure.signals().contains(ExposureSignal.NO_RESISTANCE));
    }

    /**
     * Sur deux essais, « 100 % de réussite » ne décrit qu'un hasard : sans
     * volume, le signal de masse ne doit pas se déclencher.
     */
    @Test
    void unTauxNEstPasLuSansAssezDeTentatives() {
        TargetFacts neuve = target("neuve", OperatingSystem.LINUX, Difficulty.MEDIUM, "10.10.40.10", true, 1, 1, 0);

        TargetExposure exposure = ExposureAnalyzer.analyse(List.of(neuve), true).get(0);

        assertFalse(exposure.signals().contains(ExposureSignal.WIDELY_OWNED));
        assertFalse(exposure.signals().contains(ExposureSignal.NO_RESISTANCE));
    }

    @Test
    void uneCibleJamaisEntameeLeSignale() {
        TargetFacts vierge = target("vierge", OperatingSystem.LINUX, Difficulty.HARD, "10.10.50.10", true, 0, 0, 3);

        TargetExposure exposure = ExposureAnalyzer.analyse(List.of(vierge), true).get(0);

        assertTrue(exposure.signals().contains(ExposureSignal.NEVER_BREACHED));
        assertTrue(exposure.advice().contains("first blood"));
    }

    @Test
    void deuxCiblesDuMemeSegmentSeSignalentMutuellement() {
        List<TargetExposure> exposures = ExposureAnalyzer.analyse(List.of(
                target("voisine-a", OperatingSystem.LINUX, Difficulty.MEDIUM, "10.10.10.11", true, 2, 1, 2),
                target("voisine-b", OperatingSystem.LINUX, Difficulty.MEDIUM, "10.10.10.12", true, 2, 1, 2)), true);

        assertTrue(exposures.stream().allMatch(e -> e.signals().contains(ExposureSignal.SHARED_SEGMENT)));
    }

    @Test
    void uneCibleSeuleDansSonSegmentNeLeSignalePas() {
        List<TargetExposure> exposures = ExposureAnalyzer.analyse(List.of(
                target("seule", OperatingSystem.LINUX, Difficulty.MEDIUM, "10.10.10.11", true, 0, 0, 0),
                target("ailleurs", OperatingSystem.LINUX, Difficulty.MEDIUM, "10.10.20.11", true, 0, 0, 0)), true);

        assertTrue(exposures.stream().noneMatch(e -> e.signals().contains(ExposureSignal.SHARED_SEGMENT)));
    }

    @Test
    void windowsExposeLeBureauADistance() {
        TargetFacts windows = target("bureau", OperatingSystem.WINDOWS, Difficulty.MEDIUM, "10.10.60.10", true, 0, 0,
                0);

        TargetExposure exposure = ExposureAnalyzer.analyse(List.of(windows), true).get(0);

        assertEquals(ExposedService.RDP, exposure.service());
        assertEquals(3389, exposure.service().port());
        assertTrue(exposure.signals().contains(ExposureSignal.REMOTE_DESKTOP));
    }

    @Test
    void uneMachineRetireeVoitSonExpositionRetomber() {
        TargetFacts active = target("active", OperatingSystem.LINUX, Difficulty.EASY, "10.10.70.10", false, 10, 9, 3);
        TargetFacts retiree = new TargetFacts("ancienne", "ancienne", OperatingSystem.LINUX, Difficulty.EASY,
                "10.10.80.10", true, false, 10, 9, 3, 0);

        List<TargetExposure> exposures = ExposureAnalyzer.analyse(List.of(active, retiree), true);
        TargetExposure ancienne = exposures.stream().filter(e -> e.slug().equals("ancienne")).findFirst().orElseThrow();

        assertTrue(ancienne.signals().contains(ExposureSignal.RETIRED));
        assertTrue(ancienne.score() < exposures.get(0).score());
    }

    /**
     * Le cœur de la règle : ce module lit tout le catalogue, il ne doit pas
     * devenir un moyen d'obtenir l'adresse d'une cible réservée sans abonnement.
     */
    @Test
    void sansAbonnementLAdresseDUneCibleReserveeNeSortPas() {
        TargetFacts reservee = target("reservee", OperatingSystem.LINUX, Difficulty.EASY, "10.10.90.10", true, 4, 3,
                2);

        TargetExposure verrouillee = ExposureAnalyzer.analyse(List.of(reservee), false).get(0);
        TargetExposure ouverte = ExposureAnalyzer.analyse(List.of(reservee), true).get(0);

        assertTrue(verrouillee.locked());
        assertNull(verrouillee.address());
        assertNotNull(ouverte.address());
        assertEquals("10.10.90.10", ouverte.address());
        // Le niveau reste visible : l'existence d'une cible n'est pas un secret.
        assertEquals(ouverte.level(), verrouillee.level());
    }

    @Test
    void lesCiblesSortentDeLaPlusExposeeALaMoinsExposee() {
        List<TargetExposure> exposures = ExposureAnalyzer.analyse(List.of(
                target("dure", OperatingSystem.LINUX, Difficulty.INSANE, "10.10.30.10", true, 1, 0, 40),
                target("facile", OperatingSystem.LINUX, Difficulty.VERY_EASY, "10.10.10.10", false, 18, 16, 4)), true);

        assertEquals("facile", exposures.get(0).slug());
        assertEquals("dure", exposures.get(1).slug());
    }
}
