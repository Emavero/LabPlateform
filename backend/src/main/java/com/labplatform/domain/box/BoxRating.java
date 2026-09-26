package com.labplatform.domain.box;

import com.labplatform.domain.shared.ConflictException;

import java.time.Instant;
import java.util.Objects;

/**
 * Note de difficulté donnée par un joueur à une machine.
 * <p>
 * Une machine ne se note qu'une fois possédée de bout en bout : sans les deux
 * flags, on n'a pas vu ce qu'elle demandait vraiment.
 */
public record BoxRating(Long id, Long userId, Long boxId, Difficulty difficulty, Instant ratedAt) {

    public BoxRating {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(boxId, "boxId");
        Objects.requireNonNull(difficulty, "difficulty");
        Objects.requireNonNull(ratedAt, "ratedAt");
    }

    public static BoxRating cast(Long userId, Long boxId, Difficulty difficulty, boolean pwned, Instant now) {
        if (!pwned) {
            throw new ConflictException("Notez une machine une fois ses deux flags validés");
        }
        return new BoxRating(null, userId, boxId, difficulty, now);
    }

    public static BoxRating restore(Long id, Long userId, Long boxId, Difficulty difficulty, Instant ratedAt) {
        return new BoxRating(Objects.requireNonNull(id, "id"), userId, boxId, difficulty, ratedAt);
    }
}
