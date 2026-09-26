package com.labplatform.adapter.in.web.dto;

import com.labplatform.application.port.in.box.BoxView;
import com.labplatform.application.port.in.box.FlagSubmissionResult;
import com.labplatform.application.port.in.scoring.LeaderboardEntry;
import com.labplatform.domain.box.Box;
import com.labplatform.domain.box.CommunityRating;
import com.labplatform.domain.box.Difficulty;
import com.labplatform.domain.box.FlagKind;
import com.labplatform.domain.scoring.PlayerProgress;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

/**
 * Représentations HTTP du catalogue et du tableau des scores.
 * <p>
 * Le mappage est explicite, jamais automatique : une machine porte ses deux
 * flags, et rien de tout cela ne doit se retrouver dans une réponse.
 */
public final class BoxDtos {

    private BoxDtos() {
    }

    /** Machine du catalogue, vue par le joueur qui la demande. */
    public record BoxResponse(
            String slug,
            String name,
            String os,
            String osName,
            String difficulty,
            String difficultyName,
            int userFlagPoints,
            int rootFlagPoints,
            int totalPoints,
            String synopsis,
            String ipAddress,
            String maker,
            Instant releasedAt,
            boolean retired,
            boolean userOwned,
            boolean rootOwned,
            boolean pwned,
            boolean firstBlood,
            int pointsEarned,
            Instant lastOwnedAt,
            int ratingVotes,
            double ratingAverage,
            String perceivedDifficulty,
            String perceivedDifficultyName,
            String myRating) {

        public static BoxResponse from(BoxView view) {
            Box box = view.box();
            return new BoxResponse(
                    box.getSlug(),
                    box.getName(),
                    box.getOperatingSystem().name(),
                    box.getOperatingSystem().displayName(),
                    box.getDifficulty().name(),
                    box.getDifficulty().displayName(),
                    box.pointsFor(FlagKind.USER),
                    box.pointsFor(FlagKind.ROOT),
                    box.totalPoints(),
                    box.getSynopsis(),
                    box.getIpAddress(),
                    box.getMaker(),
                    box.getReleasedAt(),
                    box.isRetired(),
                    view.isOwned(FlagKind.USER),
                    view.isOwned(FlagKind.ROOT),
                    view.isPwned(),
                    view.hasFirstBlood(),
                    view.pointsEarned(),
                    view.lastOwnedAt(),
                    view.rating().votes(),
                    view.rating().averageLevel(),
                    perceived(view.rating()),
                    perceivedName(view.rating()),
                    view.myVote() == null ? null : view.myVote().name());
        }
    }

    private static String perceived(CommunityRating rating) {
        Difficulty difficulty = rating.perceived();
        return difficulty == null ? null : difficulty.name();
    }

    private static String perceivedName(CommunityRating rating) {
        Difficulty difficulty = rating.perceived();
        return difficulty == null ? null : difficulty.displayName();
    }

    /** Note de difficulté donnée par un joueur. */
    public record RatingRequest(@NotNull(message = "La difficulté est obligatoire") Difficulty difficulty) {
    }

    /** Soumission d'un flag : le type de flag et sa valeur, rien d'autre. */
    public record FlagSubmissionRequest(
            @NotNull(message = "Le type de flag est obligatoire") FlagKind kind,
            @NotBlank(message = "Le flag est obligatoire") String flag) {
    }

    public record FlagSubmissionResponse(
            String slug,
            String name,
            String kind,
            int pointsAwarded,
            boolean firstBlood,
            boolean pwned,
            ProgressResponse progress) {

        public static FlagSubmissionResponse from(FlagSubmissionResult result) {
            return new FlagSubmissionResponse(result.boxSlug(), result.boxName(), result.kind().name(),
                    result.pointsAwarded(), result.firstBlood(), result.pwned(),
                    ProgressResponse.from(result.progress()));
        }
    }

    /** Progression du joueur connecté. */
    public record ProgressResponse(
            int points,
            int availablePoints,
            int ownedFlags,
            int totalFlags,
            int boxesPwned,
            int firstBloods,
            String rank,
            String rankName,
            String nextRank,
            String nextRankName,
            int pointsToNextRank,
            double completion) {

        public static ProgressResponse from(PlayerProgress progress) {
            return new ProgressResponse(
                    progress.points(),
                    progress.availablePoints(),
                    progress.ownedFlags(),
                    progress.totalFlags(),
                    progress.boxesPwned(),
                    progress.firstBloods(),
                    progress.rank().name(),
                    progress.rank().displayName(),
                    progress.nextRank() == null ? null : progress.nextRank().name(),
                    progress.nextRank() == null ? null : progress.nextRank().displayName(),
                    progress.pointsToNextRank(),
                    progress.completion());
        }
    }

    public record LeaderboardResponse(List<LeaderboardRow> entries) {

        public static LeaderboardResponse from(List<LeaderboardEntry> entries) {
            return new LeaderboardResponse(entries.stream().map(LeaderboardRow::from).toList());
        }
    }

    public record LeaderboardRow(int position, String handle, int points, int ownedFlags, int firstBloods,
                                 String rank, String rankName, boolean self) {

        static LeaderboardRow from(LeaderboardEntry entry) {
            return new LeaderboardRow(entry.position(), entry.handle(), entry.points(), entry.ownedFlags(),
                    entry.firstBloods(), entry.rank().name(), entry.rank().displayName(), entry.self());
        }
    }
}
