package com.labplatform.application.port.in.scoring;

import com.labplatform.domain.scoring.PlayerProgress;
import com.labplatform.domain.user.Actor;

public interface GetPlayerProgressUseCase {

    PlayerProgress progressOf(Actor actor);
}
