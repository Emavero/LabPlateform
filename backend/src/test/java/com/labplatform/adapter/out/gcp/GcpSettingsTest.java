package com.labplatform.adapter.out.gcp;

import com.labplatform.domain.shared.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Une configuration GCP incomplète doit faire échouer le démarrage, pas le
 * premier clic d'un apprenant.
 */
class GcpSettingsTest {

    private static final TargetBlueprint BLUEPRINT = new TargetBlueprint("e2-small", "images/box-{slug}", 20,
            "pd-standard", "", List.of("lab-target"));

    @Test
    void perUserModeWithoutTheLearnerPlaceholderIsRefusedAtStartup() {
        // Le pire des cas : le mode annonce une instance par apprenant, le
        // gabarit en désigne une seule, et tous l'attaquent sans le savoir.
        InvalidInputException refused = assertThrows(InvalidInputException.class,
                () -> new GcpSettings("projet", "zone", "cible", "target-{slug}", Duration.ofSeconds(60), true,
                        BLUEPRINT));

        assertTrue(refused.getMessage().contains("{user}"), refused.getMessage());
    }

    @Test
    void theSharedModeNeedsNeitherPlaceholderNorBlueprint() {
        GcpSettings settings = new GcpSettings("projet", "zone", "cible", "target-{slug}", Duration.ofSeconds(60),
                false, null);

        assertEquals("target-sentinel", settings.targetInstanceName("sentinel"));
    }

    @Test
    void theBlueprintImageFollowsTheBox() {
        GcpSettings settings = new GcpSettings("projet", "zone", "cible", "target-{slug}-{user}",
                Duration.ofSeconds(60), true, BLUEPRINT);

        assertEquals("images/box-sentinel", settings.blueprintFor("sentinel").sourceImage());
        assertEquals("target-sentinel-7", settings.targetInstanceName("sentinel", 7L));
    }

    @Test
    void aMissingProjectIsSaidPlainly() {
        InvalidInputException refused = assertThrows(InvalidInputException.class,
                () -> new GcpSettings("", "zone", "cible", "{slug}", Duration.ofSeconds(60), false, null));

        assertTrue(refused.getMessage().contains("GCP_PROJECT_ID"), refused.getMessage());
    }

    @Test
    void aPerUserModeWithoutASourceImageIsRefused() {
        // Sans image, la création échouerait devant l'apprenant.
        assertThrows(InvalidInputException.class, () -> new TargetBlueprint("e2-small", "", 20, "pd-standard",
                "", List.of()));
    }
}
