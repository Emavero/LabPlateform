package com.labplatform.application.port.out;

import com.labplatform.domain.box.BoxInstance;

import java.util.List;
import java.util.Optional;

public interface BoxInstanceRepositoryPort {

    List<BoxInstance> findByUser(Long userId);

    Optional<BoxInstance> find(Long userId, Long boxId);

    /** L'instance en cours d'exécution de ce joueur, s'il en a une. */
    Optional<BoxInstance> findRunningByUser(Long userId);

    BoxInstance save(BoxInstance instance);
}
