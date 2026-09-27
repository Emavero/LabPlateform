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

    /** Nombre total de flags validés, tous joueurs confondus. */
    long count();

    /** Totaux par joueur, les meilleurs d'abord. */
    List<PlayerScore> topScores(int limit);

    /**
     * Validations par machine et par flag, tous joueurs confondus.
     * <p>
     * Agrégé en base plutôt que compté en mémoire : l'analyse d'exposition a
     * besoin du décompte de toutes les machines, et charger chaque validation
     * pour en compter quelques totaux coûterait bien plus que la requête.
     */
    List<OwnTally> tallyByBox();

    record OwnTally(Long boxId, FlagKind kind, long count) {
    }
}
