package com.labplatform.domain.exposure;

import com.labplatform.domain.box.Difficulty;
import com.labplatform.domain.lab.OperatingSystem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttackPathFinderTest {

    private static TargetFacts target(String slug, OperatingSystem system, Difficulty difficulty, String address,
                                      boolean proOnly, long userOwns, long rootOwns, long attempts) {
        return new TargetFacts(slug, slug, system, difficulty, address, false, proOnly, userOwns, rootOwns, attempts,
                0);
    }

    private static List<AttackPath> pathsFor(List<TargetFacts> facts, boolean unlocked) {
        return AttackPathFinder.paths(ExposureAnalyzer.analyse(facts, unlocked), facts, 10);
    }

    private static AttackPath pathTo(List<AttackPath> paths, String slug) {
        return paths.stream().filter(path -> path.objectiveSlug().equals(slug)).findFirst().orElseThrow();
    }

    @Test
    void leCheminVersUneCibleCommenceParLaMoinsResistante() {
        List<TargetFacts> facts = List.of(
                target("porte", OperatingSystem.LINUX, Difficulty.VERY_EASY, "10.10.10.10", false, 18, 16, 3),
                target("citadelle", OperatingSystem.LINUX, Difficulty.INSANE, "10.10.10.20", true, 1, 0, 30));

        AttackPath path = pathTo(pathsFor(facts, true), "citadelle");

        assertEquals("porte", path.hops().get(0).slug());
        assertEquals(AttackPath.AttackLink.ENTRY, path.hops().get(0).reason());
        assertEquals("citadelle", path.hops().get(path.hops().size() - 1).slug());
    }

    @Test
    void deuxCiblesDuMemeSegmentSontRelieesParUnDeplacementLateral() {
        List<TargetFacts> facts = List.of(
                target("porte", OperatingSystem.LINUX, Difficulty.VERY_EASY, "10.10.10.10", false, 18, 16, 3),
                target("voisine", OperatingSystem.WINDOWS, Difficulty.HARD, "10.10.10.20", true, 2, 1, 6));

        AttackPath path = pathTo(pathsFor(facts, true), "voisine");

        assertEquals(AttackPath.AttackLink.SAME_SEGMENT, path.hops().get(1).reason());
    }

    /** Segments différents, même système : c'est la technique qui se réemploie. */
    @Test
    void deuxSegmentsDistinctsSeRelientParLeSysteme() {
        List<TargetFacts> facts = List.of(
                target("porte", OperatingSystem.LINUX, Difficulty.VERY_EASY, "10.10.10.10", false, 18, 16, 3),
                target("lointaine", OperatingSystem.LINUX, Difficulty.HARD, "10.10.90.20", true, 2, 1, 6));

        AttackPath path = pathTo(pathsFor(facts, true), "lointaine");

        assertEquals(AttackPath.AttackLink.SAME_SYSTEM, path.hops().get(1).reason());
    }

    /** Rien ne relie un Windows isolé à un Linux isolé : l'assaut direct reste seul. */
    @Test
    void sansLienLeCheminSeReduitALaCible() {
        List<TargetFacts> facts = List.of(
                target("porte", OperatingSystem.LINUX, Difficulty.VERY_EASY, "10.10.10.10", false, 18, 16, 3),
                target("isolee", OperatingSystem.WINDOWS, Difficulty.HARD, "10.10.90.20", true, 2, 1, 6));

        AttackPath path = pathTo(pathsFor(facts, true), "isolee");

        assertEquals(1, path.hops().size());
        assertEquals("isolee", path.hops().get(0).slug());
    }

    @Test
    void unDetourPlusCouteuxQueLAssautDirectNEstPasConseille() {
        // La cible visée est presque aussi abordable que l'étape qui y mènerait :
        // passer par elle coûterait plus que de l'attaquer de front.
        List<TargetFacts> facts = List.of(
                target("a", OperatingSystem.LINUX, Difficulty.EASY, "10.10.10.10", false, 10, 9, 2),
                target("b", OperatingSystem.LINUX, Difficulty.EASY, "10.10.10.11", false, 10, 9, 2));

        AttackPath path = pathTo(pathsFor(facts, true), "b");

        assertEquals(1, path.hops().size());
        assertEquals(AttackPathFinder.effortOf(ExposureAnalyzer.analyse(facts, true).stream()
                .filter(e -> e.slug().equals("b")).findFirst().orElseThrow()), path.effort());
    }

    /**
     * Une cible verrouillée ne figure pas dans un chemin : l'y faire apparaître
     * dévoilerait sa place dans le lab, que sa fiche ne montre pas.
     */
    @Test
    void uneCibleVerrouilleeNApparaitDansAucunChemin() {
        List<TargetFacts> facts = List.of(
                target("libre", OperatingSystem.LINUX, Difficulty.VERY_EASY, "10.10.10.10", false, 18, 16, 3),
                target("reservee", OperatingSystem.LINUX, Difficulty.HARD, "10.10.10.20", true, 2, 1, 6));

        List<AttackPath> paths = pathsFor(facts, false);

        assertTrue(paths.stream().noneMatch(path -> path.objectiveSlug().equals("reservee")));
        assertTrue(paths.stream()
                .flatMap(path -> path.hops().stream())
                .noneMatch(hop -> hop.slug().equals("reservee")));
    }

    @Test
    void lesObjectifsLesPlusExigeantsSortentDAbord() {
        List<TargetFacts> facts = List.of(
                target("porte", OperatingSystem.LINUX, Difficulty.VERY_EASY, "10.10.10.10", false, 18, 16, 3),
                target("moyenne", OperatingSystem.LINUX, Difficulty.MEDIUM, "10.10.10.15", true, 4, 2, 6),
                target("citadelle", OperatingSystem.LINUX, Difficulty.INSANE, "10.10.10.20", true, 1, 0, 30));

        List<AttackPath> paths = pathsFor(facts, true);

        assertEquals("citadelle", paths.get(0).objectiveSlug());
        assertTrue(paths.get(0).effort() >= paths.get(paths.size() - 1).effort());
    }

    @Test
    void sansCibleLisibleAucunCheminNEstRendu() {
        List<TargetFacts> facts = List.of(
                target("reservee", OperatingSystem.LINUX, Difficulty.HARD, "10.10.10.20", true, 0, 0, 0));

        assertTrue(pathsFor(facts, false).isEmpty());
    }

    @Test
    void leNombreDeCheminsEstBorne() {
        List<TargetFacts> facts = List.of(
                target("a", OperatingSystem.LINUX, Difficulty.EASY, "10.10.10.10", false, 0, 0, 0),
                target("b", OperatingSystem.LINUX, Difficulty.MEDIUM, "10.10.10.11", false, 0, 0, 0),
                target("c", OperatingSystem.LINUX, Difficulty.HARD, "10.10.10.12", false, 0, 0, 0));

        assertEquals(2, AttackPathFinder.paths(ExposureAnalyzer.analyse(facts, true), facts, 2).size());
    }

    /** La somme des étapes fait l'effort du chemin : un lecteur peut refaire l'addition. */
    @Test
    void lEffortDUnCheminEstLaSommeDeSesEtapes() {
        List<TargetFacts> facts = List.of(
                target("porte", OperatingSystem.LINUX, Difficulty.VERY_EASY, "10.10.10.10", false, 18, 16, 3),
                target("citadelle", OperatingSystem.LINUX, Difficulty.INSANE, "10.10.10.20", true, 1, 0, 30));

        for (AttackPath path : pathsFor(facts, true)) {
            assertEquals(path.hops().stream().mapToInt(AttackPath.AttackHop::effort).sum(), path.effort());
        }
    }

    @Test
    void lEffortNEstJamaisNul() {
        List<TargetFacts> facts = List.of(
                target("ouverte", OperatingSystem.WINDOWS, Difficulty.VERY_EASY, "10.10.10.10", false, 40, 40, 1),
                target("voisine", OperatingSystem.WINDOWS, Difficulty.VERY_EASY, "10.10.10.11", false, 40, 40, 1));

        assertFalse(pathsFor(facts, true).stream()
                .flatMap(path -> path.hops().stream())
                .anyMatch(hop -> hop.effort() <= 0));
    }
}
