package com.labplatform.application.port.in.profile;

import com.labplatform.domain.achievement.Achievement;
import com.labplatform.domain.user.Actor;

import java.util.List;

public interface GetAchievementsUseCase {

    /** Tous les hauts faits, obtenus ou non : les manquants disent quoi viser. */
    List<Achievement.Earned> achievements(Actor actor);
}
