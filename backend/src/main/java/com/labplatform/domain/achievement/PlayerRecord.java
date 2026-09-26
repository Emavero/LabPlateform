package com.labplatform.domain.achievement;

import com.labplatform.domain.box.Difficulty;

/**
 * Le palmarès d'un joueur, réduit à ce dont les hauts faits ont besoin.
 * Aucune requête ici : ce sont des nombres déjà calculés.
 *
 * @param hardestPwned  difficulté de la machine la plus dure possédée, nulle si aucune
 * @param tracksStarted filières dans lesquelles au moins une section est terminée
 */
public record PlayerRecord(int ownedFlags, int boxesPwned, int firstBloods, Difficulty hardestPwned,
                           int coursesCompleted, int sectionsCompleted, int tracksStarted) {

    public static final PlayerRecord EMPTY = new PlayerRecord(0, 0, 0, null, 0, 0, 0);

    /** Vrai si la machine la plus dure possédée atteint au moins ce palier. */
    public boolean pwnedAtLeast(Difficulty difficulty) {
        return hardestPwned != null && hardestPwned.ordinal() >= difficulty.ordinal();
    }
}
