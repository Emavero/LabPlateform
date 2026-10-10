package com.labplatform.adapter.out.gcp;

import com.labplatform.domain.shared.InvalidInputException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les noms d'instance sont dérivés, jamais saisis : un gabarit, un identifiant
 * de machine, un identifiant d'apprenant. Ce qui est réparable l'est en
 * silence ; ce qui ne l'est pas échoue.
 */
class InstanceNameTest {

    @Test
    void theTemplateFillsInTheBoxAndTheLearner() {
        assertEquals("target-sentinel-42", InstanceName.forTarget("target-{slug}-{user}", "sentinel", 42L));
    }

    @Test
    void twoLearnersOnTheSameBoxNeverShareAName() {
        // C'est l'invariant du modèle : deux apprenants, deux machines.
        assertNotEquals(InstanceName.forTarget("target-{slug}-{user}", "sentinel", 1L),
                InstanceName.forTarget("target-{slug}-{user}", "sentinel", 2L));
    }

    @Test
    void aLongBoxNameIsShortenedButTheLearnerIsNeverLost() {
        // Compute Engine s'arrête à 63 caractères. Tronquer le nom entier
        // donnerait le même aux deux apprenants, qui partageraient une
        // instance — le défaut que ce modèle corrige.
        String slug = "a".repeat(120);

        String first = InstanceName.forTarget("target-{slug}-{user}", slug, 1L);
        String second = InstanceName.forTarget("target-{slug}-{user}", slug, 2L);

        assertEquals(63, first.length());
        assertEquals(63, second.length());
        assertNotEquals(first, second);
        assertTrue(first.endsWith("-1"), first);
        assertTrue(second.endsWith("-2"), second);
    }

    @Test
    void whatComputeEngineRefusesIsRepairedRatherThanRejected() {
        // Majuscules, soulignés, points : un slug du catalogue peut en porter,
        // et l'apprenant n'a pas à voir une erreur d'hébergeur pour autant.
        assertEquals("target-mon-box-7", InstanceName.forTarget("target-{slug}-{user}", "Mon_Box", 7L));
        // Un nom doit commencer par une lettre.
        assertEquals("x1-box-3", InstanceName.forTarget("{slug}-{user}", "1-box", 3L));
        // Et ne pas finir par un trait d'union.
        assertEquals("target-box", InstanceName.of("target-box-"));
    }

    @Test
    void anEmptyTemplateIsAConfigurationMistakeWorthSaying() {
        assertThrows(InvalidInputException.class, () -> InstanceName.forTarget("", "sentinel", 1L));
        assertThrows(InvalidInputException.class, () -> InstanceName.forTarget(null, "sentinel", 1L));
    }
}
