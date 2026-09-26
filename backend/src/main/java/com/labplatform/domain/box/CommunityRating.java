package com.labplatform.domain.box;

import java.util.EnumMap;
import java.util.Map;

/**
 * Difficulté ressentie par ceux qui ont fait la machine, à côté de celle
 * annoncée par son auteur. L'écart entre les deux est l'information utile.
 *
 * @param votes        nombre de votes
 * @param averageLevel moyenne sur l'échelle des difficultés, de 1 à 5 ;
 *                     0 s'il n'y a aucun vote
 */
public record CommunityRating(int votes, double averageLevel) {

    public static final CommunityRating NONE = new CommunityRating(0, 0);

    public static CommunityRating of(Map<Difficulty, Integer> votesByDifficulty) {
        Map<Difficulty, Integer> tally = new EnumMap<>(votesByDifficulty);
        int total = tally.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            return NONE;
        }
        int weighted = tally.entrySet().stream()
                .mapToInt(entry -> level(entry.getKey()) * entry.getValue())
                .sum();
        return new CommunityRating(total, (double) weighted / total);
    }

    /** Rang de la difficulté sur l'échelle, de 1 (très facile) à 5 (insane). */
    public static int level(Difficulty difficulty) {
        return difficulty.ordinal() + 1;
    }

    /** Difficulté la plus proche de la moyenne des votes, vide sans vote. */
    public Difficulty perceived() {
        if (votes == 0) {
            return null;
        }
        int rounded = (int) Math.round(averageLevel);
        int index = Math.min(Math.max(rounded, 1), Difficulty.values().length) - 1;
        return Difficulty.values()[index];
    }
}
