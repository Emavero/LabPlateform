package com.labplatform.domain.scoring;

/**
 * Progression d'un joueur dans le catalogue : ce que la plateforme sait dire
 * de lui sans rien révéler des machines qu'il n'a pas encore faites.
 *
 * @param points          points acquis
 * @param availablePoints points que pèse le catalogue entier
 * @param ownedFlags      flags validés
 * @param totalFlags      flags que compte le catalogue
 * @param boxesPwned      machines possédées de bout en bout (user + root)
 * @param firstBloods     flags validés avant tout le monde
 */
public record PlayerProgress(int points, int availablePoints, int ownedFlags, int totalFlags, int boxesPwned,
                             int firstBloods, Rank rank, Rank nextRank, int pointsToNextRank) {

    public static PlayerProgress of(int points, int availablePoints, int ownedFlags, int totalFlags,
                                    int boxesPwned, int firstBloods) {
        double ratio = availablePoints == 0 ? 0 : (double) points / availablePoints;
        Rank rank = Rank.forCompletion(ratio);
        Rank next = rank.next().orElse(null);
        int missing = next == null ? 0 : pointsNeededFor(next, availablePoints) - points;
        return new PlayerProgress(points, availablePoints, ownedFlags, totalFlags, boxesPwned, firstBloods,
                rank, next, Math.max(missing, 0));
    }

    /** Part du catalogue possédée, entre 0 et 1. */
    public double completion() {
        return availablePoints == 0 ? 0 : (double) points / availablePoints;
    }

    private static int pointsNeededFor(Rank rank, int availablePoints) {
        return (int) Math.ceil(rank.requiredPercent() / 100.0 * availablePoints);
    }
}
