package com.labplatform.application.port.in.academy;

import com.labplatform.domain.user.Actor;

import java.util.List;

public interface GetLearningProgressUseCase {

    /** Une ligne par filière, dans l'ordre du menu. */
    List<LearningProgress> learningProgress(Actor actor);
}
