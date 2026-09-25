package com.labplatform.application.port.in.scoring;

import com.labplatform.domain.user.Actor;

import java.util.List;

public interface GetLeaderboardUseCase {

    List<LeaderboardEntry> leaderboard(Actor actor, int limit);
}
