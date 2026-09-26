package com.labplatform.application.port.out;

import com.labplatform.domain.box.BoxRating;
import com.labplatform.domain.box.Difficulty;

import java.util.List;
import java.util.Optional;

public interface BoxRatingRepositoryPort {

    Optional<BoxRating> find(Long userId, Long boxId);

    /** Enregistre le vote, ou remplace celui que le joueur avait déjà donné. */
    BoxRating save(BoxRating rating);

    /** Dépouillement complet : une ligne par (machine, difficulté) votée. */
    List<Tally> tallies();

    record Tally(Long boxId, Difficulty difficulty, int votes) {
    }
}
