package com.labplatform.domain.box;

import java.time.Instant;
import java.util.Objects;

/**
 * Possession d'un flag par un utilisateur, à la date de sa soumission.
 * <p>
 * Les points sont figés au moment de la validation : rééquilibrer une
 * machine plus tard ne réécrit pas le passé des joueurs.
 */
public record Own(Long id, Long userId, Long boxId, FlagKind kind, int points, boolean firstBlood, Instant ownedAt) {

    public Own {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(boxId, "boxId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(ownedAt, "ownedAt");
        if (points < 0) {
            throw new IllegalArgumentException("Des points négatifs n'ont pas de sens : " + points);
        }
    }

    /** Nouvelle possession, pas encore persistée. Voir {@link Box#claim}. */
    static Own record(Long userId, Long boxId, FlagKind kind, int points, boolean firstBlood, Instant ownedAt) {
        return new Own(null, userId, boxId, kind, points, firstBlood, ownedAt);
    }

    public static Own restore(Long id, Long userId, Long boxId, FlagKind kind, int points, boolean firstBlood,
                              Instant ownedAt) {
        return new Own(Objects.requireNonNull(id, "id"), userId, boxId, kind, points, firstBlood, ownedAt);
    }
}
