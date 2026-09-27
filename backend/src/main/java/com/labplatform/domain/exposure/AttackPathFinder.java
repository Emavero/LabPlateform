package com.labplatform.domain.exposure;

import com.labplatform.domain.exposure.AttackPath.AttackHop;
import com.labplatform.domain.exposure.AttackPath.AttackLink;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Construit les chemins de progression entre les cibles du lab.
 * <p>
 * Le graphe est explicite : un arc relie deux cibles quand un progrès sur la
 * première rend la seconde plus abordable, soit parce qu'elles partagent un
 * segment réseau — déplacement latéral — soit parce qu'elles tournent sur le
 * même système, où les techniques et les identifiants se réemploient. L'arc va
 * toujours du moins résistant au plus résistant : le graphe est donc sans
 * cycle, et le plus court chemin s'obtient en une passe, sans Dijkstra ni file
 * de priorité.
 * <p>
 * Tout le modèle tient dans une idée : une cible atteinte depuis une autre coûte
 * moins cher qu'attaquée de front. Sans cette remise, passer par une étape
 * coûterait toujours plus que l'assaut direct, et « le plus court chemin »
 * n'aurait jamais qu'une étape — ce qui ne conseillerait rien. La remise est
 * plus forte depuis le même segment, où l'on est déjà dans le réseau, que
 * depuis un simple voisinage de système, où seule la technique se réemploie.
 * <p>
 * Fonction pure, comme l'analyse d'exposition : les faits entrent, les chemins
 * sortent.
 */
public final class AttackPathFinder {

    /** Effort plancher : aucune cible ne se prend « pour rien ». */
    private static final int MIN_EFFORT = 5;
    /** Part de l'effort restant depuis une cible du même segment : on est dans le réseau. */
    private static final int SAME_SEGMENT_COST_PERCENT = 55;
    /** Depuis un même système sur un autre segment : la technique se réemploie, l'accès non. */
    private static final int SAME_SYSTEM_COST_PERCENT = 75;

    private AttackPathFinder() {
    }

    /** Effort d'une cible : l'inverse de son exposition, jamais nul. */
    public static int effortOf(TargetExposure exposure) {
        return Math.max(MIN_EFFORT, 100 - exposure.score());
    }

    /**
     * Chemins vers chaque cible, les objectifs les plus exigeants d'abord.
     * <p>
     * Les cibles verrouillées pour ce lecteur sont écartées du graphe : les
     * faire figurer comme étape dévoilerait leur place dans le lab, que leur
     * fiche ne montre pas.
     *
     * @param limit nombre de chemins rendus
     */
    public static List<AttackPath> paths(List<TargetExposure> exposures, List<TargetFacts> facts, int limit) {
        Map<String, TargetFacts> byslug = new HashMap<>();
        facts.forEach(fact -> byslug.put(fact.slug(), fact));

        // Du moins résistant au plus résistant : c'est l'ordre dans lequel on
        // progresse, et celui qui rend le graphe acyclique.
        List<TargetExposure> ordered = exposures.stream()
                .filter(exposure -> !exposure.locked())
                .sorted(Comparator.comparingInt(AttackPathFinder::effortOf)
                        .thenComparing(TargetExposure::name))
                .toList();
        if (ordered.isEmpty()) {
            return List.of();
        }

        Map<String, Integer> best = new HashMap<>();
        Map<String, List<AttackHop>> trail = new HashMap<>();

        for (TargetExposure target : ordered) {
            int effort = effortOf(target);
            AttackHop entry = hop(target, byslug.get(target.slug()), AttackLink.ENTRY, effort);
            // Attaquer la cible de front est toujours une option : c'est la
            // borne à battre pour tout chemin qui passerait par ailleurs.
            best.put(target.slug(), effort);
            trail.put(target.slug(), List.of(entry));

            for (TargetExposure earlier : ordered) {
                if (earlier.slug().equals(target.slug()) || effortOf(earlier) > effort) {
                    continue;
                }
                AttackLink link = linkBetween(earlier, target);
                if (link == null) {
                    continue;
                }
                int through = best.getOrDefault(earlier.slug(), Integer.MAX_VALUE);
                if (through == Integer.MAX_VALUE) {
                    continue;
                }
                // Un détour ne se prend que s'il coûte moins que l'assaut direct :
                // sinon le chemin conseillé serait plus long que le raccourci.
                int counted = discounted(effort, link);
                int total = through + counted;
                if (total < best.get(target.slug())) {
                    List<AttackHop> hops = new ArrayList<>(trail.get(earlier.slug()));
                    hops.add(hop(target, byslug.get(target.slug()), link, counted));
                    best.put(target.slug(), total);
                    trail.put(target.slug(), hops);
                }
            }
        }

        return ordered.stream()
                .map(target -> new AttackPath(target.slug(), target.name(), trail.get(target.slug()),
                        best.get(target.slug())))
                // Les objectifs les plus exigeants d'abord : ce sont ceux dont le
                // chemin apprend quelque chose.
                .sorted(Comparator.comparingInt(AttackPath::effort).reversed()
                        .thenComparing(AttackPath::objectiveName))
                .limit(limit)
                .toList();
    }

    /** Effort d'une cible atteinte par ce lien, remise comprise. */
    private static int discounted(int effort, AttackLink link) {
        int percent = link == AttackLink.SAME_SEGMENT ? SAME_SEGMENT_COST_PERCENT : SAME_SYSTEM_COST_PERCENT;
        return Math.max(MIN_EFFORT, effort * percent / 100);
    }

    /** Lien entre deux cibles, ou null quand rien ne les relie. */
    private static AttackLink linkBetween(TargetExposure from, TargetExposure to) {
        if (!from.segment().isBlank() && from.segment().equals(to.segment())) {
            return AttackLink.SAME_SEGMENT;
        }
        return from.service() == to.service() ? AttackLink.SAME_SYSTEM : null;
    }

    /**
     * Étape du chemin. L'effort porté est celui réellement compté dans le total,
     * remise comprise : la somme des étapes fait donc l'effort du chemin, et un
     * lecteur peut refaire l'addition.
     */
    private static AttackHop hop(TargetExposure exposure, TargetFacts fact, AttackLink link, int countedEffort) {
        return new AttackHop(exposure.slug(), exposure.name(), exposure.service(),
                fact == null ? exposure.segment() : fact.segment(), countedEffort, link);
    }
}
