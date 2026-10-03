package com.labplatform.domain.academy;

import com.labplatform.domain.shared.InvalidInputException;
import com.labplatform.domain.shared.NotFoundException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le sous-domaine, le chemin d'attaque, le cas d'usage et les concepteurs d'un
 * cours. Ces règles-là ne dépendent ni de Spring, ni d'une base.
 */
class CourseBriefingTest {

    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");

    // ------------------------------------------------------------ Sous-domaine

    @Test
    void aTopicCarriesItsTrack() {
        assertEquals(Track.FORENSICS, CourseTopic.MEMORY_ANALYSIS.track());
        assertEquals(Track.DEFENSE, CourseTopic.SIEM_SOC.track());
    }

    @Test
    void aTrackListsOnlyItsOwnTopics() {
        assertTrue(CourseTopic.of(Track.FORENSICS).stream().allMatch(t -> t.track() == Track.FORENSICS));
        assertTrue(CourseTopic.of(Track.DEFENSE).stream().allMatch(t -> t.track() == Track.DEFENSE));
        assertEquals(CourseTopic.values().length,
                CourseTopic.of(Track.FORENSICS).size() + CourseTopic.of(Track.DEFENSE).size());
    }

    @Test
    void everyTopicHasADistinctSlug() {
        long distinct = java.util.Arrays.stream(CourseTopic.values()).map(CourseTopic::slug).distinct().count();
        assertEquals(CourseTopic.values().length, distinct);
    }

    @Test
    void aTopicIsFoundByItsSlug() {
        assertEquals(CourseTopic.MEMORY_ANALYSIS, CourseTopic.ofSlug("analyse-memoire"));
        assertEquals(CourseTopic.HARDENING, CourseTopic.ofSlug("  DURCISSEMENT  "));
        assertThrows(NotFoundException.class, () -> CourseTopic.ofSlug("stéganographie"));
    }

    @Test
    void aCourseTakesItsTrackFromItsTopic() {
        // La filière n'est pas saisie : un cours ne peut donc pas se contredire.
        assertEquals(Track.FORENSICS, course(CourseTopic.MEMORY_ANALYSIS, CourseBriefing.empty()).getTrack());
        assertEquals(Track.DEFENSE, course(CourseTopic.FIREWALLS, CourseBriefing.empty()).getTrack());
    }

    // ------------------------------------------------------ Chemin d'attaque

    @Test
    void anAttackPathIsEitherAbsentOrComplete() {
        assertFalse(AttackPath.none().isPresent());
        assertThrows(InvalidInputException.class, () -> AttackPath.of("Un résumé sans étapes", List.of()));
        assertThrows(InvalidInputException.class,
                () -> AttackPath.of(null, List.of(stage(1, "Accès initial"))));
    }

    @Test
    void anAttackPathOrdersItsStagesByPosition() {
        AttackPath path = AttackPath.of("Chaîne",
                List.of(stage(3, "Exfiltration"), stage(1, "Accès initial"), stage(2, "Élévation")));

        assertEquals(List.of("Accès initial", "Élévation", "Exfiltration"),
                path.stages().stream().map(AttackStage::name).toList());
    }

    @Test
    void aStageNeedsANameAndADescription() {
        assertThrows(InvalidInputException.class, () -> AttackStage.of(null, 1, "  ", "Description", null));
        assertThrows(InvalidInputException.class, () -> AttackStage.of(null, 1, "Accès", "  ", null));
        assertThrows(InvalidInputException.class, () -> AttackStage.of(null, 0, "Accès", "Description", null));
    }

    @Test
    void aStageTechniqueIsOptional() {
        assertEquals(null, AttackStage.of(null, 1, "Accès", "Description", "   ").technique());
        assertEquals("T1078", AttackStage.of(null, 1, "Accès", "Description", " T1078 ").technique());
    }

    // -------------------------------------------------------- Cas d'usage réel

    @Test
    void aRealCaseIsDescribedInFullOrNotAtAll() {
        assertFalse(RealWorldCase.none().isPresent());
        assertTrue(RealWorldCase.of("Banque", "Situation", "Enjeu", "Issue").isPresent());
        assertThrows(InvalidInputException.class, () -> RealWorldCase.of("Banque", "Situation", null, null));
    }

    @Test
    void aRealCaseMadeOfBlanksIsAnAbsentCase() {
        // Un formulaire ouvert puis refermé sans rien saisir n'est pas une erreur.
        assertFalse(RealWorldCase.of("  ", "", null, "   ").isPresent());
    }

    // ----------------------------------------------------------- Concepteurs

    @Test
    void aDesignerNeedsANameAndARole() {
        assertThrows(InvalidInputException.class, () -> CourseDesigner.of(null, 1, "  ", "Analyste", null));
        assertThrows(InvalidInputException.class, () -> CourseDesigner.of(null, 1, "Awa Diallo", " ", null));
    }

    @Test
    void aDesignerWithoutAvatarIsShownByInitials() {
        CourseDesigner designer = CourseDesigner.of(null, 1, "Awa Diallo", "Analyste forensique", null);

        assertEquals(null, designer.avatarUrl());
        assertEquals("AD", designer.initials());
        assertEquals("M", CourseDesigner.of(null, 1, "Marc", "Expert", null).initials());
    }

    @Test
    void designersAreOrderedByPosition() {
        CourseBriefing briefing = new CourseBriefing(AttackPath.none(), RealWorldCase.none(),
                List.of(CourseDesigner.of(null, 2, "Second", "Rôle", null),
                        CourseDesigner.of(null, 1, "Premier", "Rôle", null)));

        assertEquals(List.of("Premier", "Second"), briefing.designers().stream().map(CourseDesigner::name).toList());
    }

    // --------------------------------------------------------------- Dossier

    @Test
    void anEmptyBriefingHasNothingToShow() {
        assertFalse(CourseBriefing.empty().isPresent());
        assertFalse(course(CourseTopic.HARDENING, null).getBriefing().isPresent());
    }

    @Test
    void aBriefingWithAnySinglePieceIsWorthShowing() {
        assertTrue(new CourseBriefing(AttackPath.of("Chaîne", List.of(stage(1, "Accès"))), null, null).isPresent());
        assertTrue(new CourseBriefing(null, RealWorldCase.of("Banque", "S", "E", "I"), null).isPresent());
        assertTrue(new CourseBriefing(null, null,
                List.of(CourseDesigner.of(null, 1, "Awa Diallo", "Analyste", null))).isPresent());
    }

    private static AttackStage stage(int position, String name) {
        return AttackStage.of(null, position, name, "Ce que fait l'attaquant.", null);
    }

    private static Course course(CourseTopic topic, CourseBriefing briefing) {
        return Course.create("un-cours", "Un cours", topic, CourseLevel.EASY, "Résumé.", NOW,
                List.of(CourseSection.of(null, "intro", "Intro", SectionKind.THEORY, 1, 10, "Contenu.")),
                briefing);
    }
}
