package com.labplatform.domain.scenario;

import com.labplatform.domain.box.FlagKind;

import java.util.Map;
import java.util.Set;

/**
 * Ce qu'un joueur a obtenu, tel que l'agrégat en a besoin pour dire où il en
 * est.
 * <p>
 * Rassemblé une fois par la couche applicative et passé à l'agrégat : sans cela,
 * {@link Scenario} devrait interroger des dépôts, et un agrégat qui va chercher
 * ses données ne se teste plus sans base.
 *
 * @param flagsByBox     flags validés, par lien de machine
 * @param finishedCourses liens des cours entièrement terminés
 */
public record ScenarioEvidence(Map<String, Set<FlagKind>> flagsByBox, Set<String> finishedCourses) {

    public static final ScenarioEvidence NONE = new ScenarioEvidence(Map.of(), Set.of());

    /** L'étape est-elle franchie ? */
    public boolean satisfies(ScenarioStep step) {
        if (step.kind() == ScenarioStepKind.COURSE) {
            return finishedCourses.contains(step.reference());
        }
        if (step.objective() == null) {
            return false;
        }
        Set<FlagKind> flags = flagsByBox.getOrDefault(step.reference(), Set.of());
        return switch (step.objective()) {
            case USER_FLAG -> flags.contains(FlagKind.USER);
            case ROOT_FLAG -> flags.contains(FlagKind.ROOT);
            case BOTH_FLAGS -> flags.contains(FlagKind.USER) && flags.contains(FlagKind.ROOT);
        };
    }
}
