package com.labplatform.domain.academy;

import java.util.Comparator;
import java.util.List;

/**
 * Ce qui entoure les sections d'un cours : la chaîne d'attaque qu'il étudie, la
 * situation réelle où elle se rencontre, et ceux qui l'ont écrite.
 * <p>
 * Ces trois pièces voyagent ensemble parce qu'elles se lisent ensemble, et
 * qu'aucune n'est nécessaire au fonctionnement du cours : les regrouper évite
 * d'allonger les fabriques de {@link Course} de trois paramètres facultatifs.
 */
public record CourseBriefing(AttackPath attackPath, RealWorldCase realCase, List<CourseDesigner> designers) {

    private static final CourseBriefing EMPTY = new CourseBriefing(AttackPath.none(), RealWorldCase.none(), List.of());

    public CourseBriefing {
        attackPath = attackPath == null ? AttackPath.none() : attackPath;
        realCase = realCase == null ? RealWorldCase.none() : realCase;
        designers = designers == null ? List.of() : designers.stream()
                .sorted(Comparator.comparingInt(CourseDesigner::position))
                .toList();
    }

    public static CourseBriefing empty() {
        return EMPTY;
    }

    /** Y a-t-il quelque chose à afficher au-delà des sections ? */
    public boolean isPresent() {
        return attackPath.isPresent() || realCase.isPresent() || !designers.isEmpty();
    }
}
