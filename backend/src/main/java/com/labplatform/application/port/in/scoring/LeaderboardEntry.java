package com.labplatform.application.port.in.scoring;

import com.labplatform.domain.scoring.Rank;

/**
 * Ligne du classement public.
 *
 * @param position rang dans le classement, à partir de 1
 * @param handle   pseudonyme affiché : jamais l'adresse e-mail complète
 * @param self     vrai pour la ligne de l'appelant, que l'interface met en avant
 */
public record LeaderboardEntry(int position, String handle, int points, int ownedFlags, int firstBloods,
                               Rank rank, boolean self) {
}
