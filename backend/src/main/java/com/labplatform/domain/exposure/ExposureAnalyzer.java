package com.labplatform.domain.exposure;

import com.labplatform.domain.box.Difficulty;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Note l'exposition des cibles du lab.
 * <p>
 * Fonction pure : elle reçoit des faits, elle rend des notes. Aucune base,
 * aucune horloge, aucun réseau — c'est ce qui permet de la relire, de la
 * discuter et de la tester ligne à ligne, ce qu'un score enfoui dans une
 * requête SQL n'aurait pas permis.
 * <p>
 * Les poids sont nommés et réunis ici : changer l'importance d'un signal est une
 * décision qui se voit en diff, pas un nombre à retrouver dans une formule.
 */
public final class ExposureAnalyzer {

    /** Une cible ouverte à tous est la porte d'entrée du lab : c'est le poids le plus lourd. */
    private static final int OPEN_TO_ALL_WEIGHT = 22;
    private static final int LOW_DIFFICULTY_WEIGHT = 18;
    private static final int WIDELY_OWNED_WEIGHT = 20;
    private static final int FAST_ESCALATION_WEIGHT = 16;
    private static final int NO_RESISTANCE_WEIGHT = 12;
    private static final int SHARED_SEGMENT_WEIGHT = 8;
    private static final int REMOTE_DESKTOP_WEIGHT = 6;
    /** Retirée du catalogue : moins de monde la tente, l'exposition retombe. */
    private static final int RETIRED_RELIEF = 15;

    /** Au-dessus de ce taux, « la plupart de ceux qui l'ont tentée » est mérité. */
    private static final double WIDELY_OWNED_RATE = 0.6;
    private static final double FAST_ESCALATION_RATE = 0.75;
    /** Moins d'un refus par validation : rien ne freine sérieusement. */
    private static final double NO_RESISTANCE_RATIO = 1.0;
    /** En dessous de cinq tentatives, un taux ne veut rien dire. */
    private static final long ENOUGH_ATTEMPTS = 5;

    private ExposureAnalyzer() {
    }

    /**
     * Analyse toutes les cibles, les plus exposées d'abord.
     *
     * @param unlocked le lecteur a-t-il droit au détail des cibles réservées ?
     *                 Faux : leur adresse est retirée, comme sur leur fiche.
     */
    public static List<TargetExposure> analyse(List<TargetFacts> targets, boolean unlocked) {
        Map<String, Long> perSegment = targets.stream()
                .filter(target -> !target.segment().isBlank())
                .collect(Collectors.groupingBy(TargetFacts::segment, Collectors.counting()));

        return targets.stream()
                .map(target -> score(target, perSegment.getOrDefault(target.segment(), 1L), unlocked))
                .sorted(Comparator.comparingInt(TargetExposure::score).reversed()
                        .thenComparing(TargetExposure::name))
                .toList();
    }

    private static TargetExposure score(TargetFacts target, long inSegment, boolean unlocked) {
        List<ExposureSignal> signals = new ArrayList<>();
        int score = 0;

        if (!target.proOnly()) {
            signals.add(ExposureSignal.OPEN_TO_ALL);
            score += OPEN_TO_ALL_WEIGHT;
        }
        if (target.difficulty() == Difficulty.VERY_EASY || target.difficulty() == Difficulty.EASY) {
            signals.add(ExposureSignal.LOW_DIFFICULTY);
            score += LOW_DIFFICULTY_WEIGHT;
        }
        // Les taux ne sont lus qu'au-delà d'un seuil de tentatives : sur deux
        // essais, « 100 % de réussite » ne décrit qu'un hasard.
        boolean measurable = target.userOwns() + target.attempts() >= ENOUGH_ATTEMPTS;
        if (measurable && ownRate(target) >= WIDELY_OWNED_RATE) {
            signals.add(ExposureSignal.WIDELY_OWNED);
            score += WIDELY_OWNED_WEIGHT;
        }
        if (target.userOwns() > 0 && target.escalationRate() >= FAST_ESCALATION_RATE) {
            signals.add(ExposureSignal.FAST_ESCALATION);
            score += FAST_ESCALATION_WEIGHT;
        }
        if (measurable && target.resistance() < NO_RESISTANCE_RATIO) {
            signals.add(ExposureSignal.NO_RESISTANCE);
            score += NO_RESISTANCE_WEIGHT;
        }
        if (inSegment > 1) {
            signals.add(ExposureSignal.SHARED_SEGMENT);
            score += SHARED_SEGMENT_WEIGHT;
        }
        if (target.service() == ExposedService.RDP) {
            signals.add(ExposureSignal.REMOTE_DESKTOP);
            score += REMOTE_DESKTOP_WEIGHT;
        }
        if (target.userOwns() == 0 && target.rootOwns() == 0) {
            signals.add(ExposureSignal.NEVER_BREACHED);
        }
        if (target.retired()) {
            signals.add(ExposureSignal.RETIRED);
            score -= RETIRED_RELIEF;
        }

        int bounded = Math.max(0, Math.min(100, score));
        ExposureLevel level = ExposureLevel.of(bounded);
        boolean locked = target.proOnly() && !unlocked;
        return new TargetExposure(target.slug(), target.name(), target.service(), target.segment(),
                locked ? null : target.address(), bounded, level, signals, advice(target, level, locked), locked);
    }

    private static double ownRate(TargetFacts target) {
        long tried = target.userOwns() + target.attempts();
        return tried == 0 ? 0 : (double) target.userOwns() / tried;
    }

    /**
     * Conduite à tenir. Elle s'adresse au lecteur, donc elle change avec ce
     * qu'il peut faire : sans abonnement, le conseil utile est de s'abonner, pas
     * de commencer par une cible dont il n'aura pas l'adresse.
     */
    private static String advice(TargetFacts target, ExposureLevel level, boolean locked) {
        if (locked) {
            return "Réservée aux abonnés Pro : son adresse et son détail s'ouvrent avec l'abonnement.";
        }
        if (target.userOwns() == 0 && target.rootOwns() == 0) {
            return "Personne n'y est encore entré : le first blood est à prendre.";
        }
        return switch (level) {
            case CRITICAL -> "Point d'entrée le plus rapide du lab : commencez par elle si vous débutez.";
            case HIGH -> "Entrée accessible, élévation à travailler : bon terrain d'entraînement.";
            case MODERATE -> "Résiste à l'entrée : préparez la reconnaissance avant de la lancer.";
            case LOW -> "Cible exigeante : les cours de la filière correspondante préparent mieux qu'un essai direct.";
        };
    }
}
