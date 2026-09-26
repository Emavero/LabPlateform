package com.labplatform.adapter.in.web.dto;

import com.labplatform.application.port.in.profile.ActivityEntry;
import com.labplatform.domain.achievement.Achievement;

import java.time.Instant;
import java.util.List;

/** Représentations HTTP du profil : hauts faits et activité. */
public final class ProfileDtos {

    private ProfileDtos() {
    }

    public record AchievementResponse(String code, String name, String requirement, boolean earned) {

        public static AchievementResponse from(Achievement.Earned earned) {
            return new AchievementResponse(earned.achievement().name(), earned.achievement().displayName(),
                    earned.achievement().requirement(), earned.earned());
        }

        public static List<AchievementResponse> from(List<Achievement.Earned> earned) {
            return earned.stream().map(AchievementResponse::from).toList();
        }
    }

    public record ActivityResponse(String kind, String title, String detail, int points, boolean firstBlood,
                                   Instant at) {

        public static ActivityResponse from(ActivityEntry entry) {
            return new ActivityResponse(entry.kind().name(), entry.title(), entry.detail(), entry.points(),
                    entry.firstBlood(), entry.at());
        }

        public static List<ActivityResponse> from(List<ActivityEntry> entries) {
            return entries.stream().map(ActivityResponse::from).toList();
        }
    }
}
