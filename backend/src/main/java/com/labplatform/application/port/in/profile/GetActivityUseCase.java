package com.labplatform.application.port.in.profile;

import com.labplatform.domain.user.Actor;

import java.util.List;

public interface GetActivityUseCase {

    /** Activité récente de l'appelant, la plus récente d'abord. */
    List<ActivityEntry> activity(Actor actor, int limit);
}
