package com.labplatform.adapter.in.web;

import com.labplatform.adapter.in.web.dto.ProfileDtos.AchievementResponse;
import com.labplatform.adapter.in.web.dto.ProfileDtos.ActivityResponse;
import com.labplatform.adapter.in.web.security.AuthenticatedUser;
import com.labplatform.application.port.in.profile.GetAchievementsUseCase;
import com.labplatform.application.port.in.profile.GetActivityUseCase;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private static final int DEFAULT_ACTIVITY_SIZE = 20;

    private final GetAchievementsUseCase achievements;
    private final GetActivityUseCase activity;

    public ProfileController(GetAchievementsUseCase achievements, GetActivityUseCase activity) {
        this.achievements = achievements;
        this.activity = activity;
    }

    @GetMapping("/achievements")
    public List<AchievementResponse> achievements(@AuthenticationPrincipal AuthenticatedUser user) {
        return AchievementResponse.from(achievements.achievements(user.toActor()));
    }

    @GetMapping("/activity")
    public List<ActivityResponse> activity(@AuthenticationPrincipal AuthenticatedUser user,
                                           @RequestParam(required = false) Integer limit) {
        return ActivityResponse.from(
                activity.activity(user.toActor(), limit == null ? DEFAULT_ACTIVITY_SIZE : limit));
    }
}
