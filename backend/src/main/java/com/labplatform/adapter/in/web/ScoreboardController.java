package com.labplatform.adapter.in.web;

import com.labplatform.adapter.in.web.dto.BoxDtos.LeaderboardResponse;
import com.labplatform.adapter.in.web.dto.BoxDtos.ProgressResponse;
import com.labplatform.adapter.in.web.security.AuthenticatedUser;
import com.labplatform.application.port.in.scoring.GetLeaderboardUseCase;
import com.labplatform.application.port.in.scoring.GetPlayerProgressUseCase;
import com.labplatform.config.AppProperties;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scoreboard")
public class ScoreboardController {

    private final GetPlayerProgressUseCase progress;
    private final GetLeaderboardUseCase leaderboard;
    private final AppProperties properties;

    public ScoreboardController(GetPlayerProgressUseCase progress, GetLeaderboardUseCase leaderboard,
                                AppProperties properties) {
        this.progress = progress;
        this.leaderboard = leaderboard;
        this.properties = properties;
    }

    /** Progression du joueur connecté : points, rang, machines possédées. */
    @GetMapping("/me")
    public ProgressResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return ProgressResponse.from(progress.progressOf(user.toActor()));
    }

    /** Classement public. La taille demandée est bornée par le service. */
    @GetMapping
    public LeaderboardResponse top(@AuthenticationPrincipal AuthenticatedUser user,
                                   @RequestParam(required = false) Integer limit) {
        int size = limit == null ? properties.getBoxes().getLeaderboardSize() : limit;
        return LeaderboardResponse.from(leaderboard.leaderboard(user.toActor(), size));
    }
}
