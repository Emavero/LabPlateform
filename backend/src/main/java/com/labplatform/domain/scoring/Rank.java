package com.labplatform.domain.scoring;

import java.util.Optional;

/**
 * Rang d'un joueur. Il se calcule sur la part du catalogue possédée, et non
 * sur un total de points absolu : ajouter des machines ne dégrade donc le
 * rang de personne, et le sommet reste atteignable quelle que soit la
 * taille du catalogue.
 */
public enum Rank {

    NOOB("Noob", 0),
    SCRIPT_KIDDIE("Script Kiddie", 5),
    HACKER("Hacker", 15),
    PRO_HACKER("Pro Hacker", 35),
    ELITE_HACKER("Elite Hacker", 55),
    GURU("Guru", 75),
    OMNISCIENT("Omniscient", 100);

    private final String displayName;
    private final int requiredPercent;

    Rank(String displayName, int requiredPercent) {
        this.displayName = displayName;
        this.requiredPercent = requiredPercent;
    }

    /** Rang correspondant à une part de catalogue possédée, exprimée entre 0 et 1. */
    public static Rank forCompletion(double ratio) {
        if (Double.isNaN(ratio) || ratio <= 0) {
            return NOOB;
        }
        double percent = Math.min(ratio, 1) * 100;
        Rank reached = NOOB;
        for (Rank rank : values()) {
            if (percent + 1e-9 >= rank.requiredPercent) {
                reached = rank;
            }
        }
        return reached;
    }

    /** Rang suivant, vide pour le dernier palier. */
    public Optional<Rank> next() {
        int index = ordinal() + 1;
        return index < values().length ? Optional.of(values()[index]) : Optional.empty();
    }

    public String displayName() {
        return displayName;
    }

    public int requiredPercent() {
        return requiredPercent;
    }
}
