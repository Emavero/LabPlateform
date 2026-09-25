package com.labplatform.domain.scoring;

import java.time.Instant;

/**
 * Total brut d'un joueur, tel que la persistance sait l'agréger. Le
 * classement (position, rang, pseudonyme affiché) est calculé au-dessus.
 *
 * @param lastOwnAt date de la dernière validation, qui départage deux
 *                  joueurs à égalité de points : le premier arrivé passe devant.
 */
public record PlayerScore(Long userId, String email, int points, int ownedFlags, int firstBloods, Instant lastOwnAt) {
}
