package com.labplatform.application.port.out;

import com.labplatform.domain.box.FlagKind;
import com.labplatform.domain.box.Own;
import com.labplatform.domain.scoring.PlayerScore;

import java.util.List;

public interface OwnRepositoryPort {

    List<Own> findByUser(Long userId);

    boolean exists(Long userId, Long boxId, FlagKind kind);

    /** Personne n'a encore validé ce flag : la prochaine validation sera un first blood. */
    boolean noneYet(Long boxId, FlagKind kind);

    Own save(Own own);

    /** Totaux par joueur, les meilleurs d'abord. */
    List<PlayerScore> topScores(int limit);
}
