package com.labplatform.application.service;

import com.labplatform.application.port.in.scoring.GetLeaderboardUseCase;
import com.labplatform.application.port.in.scoring.GetPlayerProgressUseCase;
import com.labplatform.application.port.in.scoring.LeaderboardEntry;
import com.labplatform.application.port.out.BoxRepositoryPort;
import com.labplatform.application.port.out.OwnRepositoryPort;
import com.labplatform.domain.box.Box;
import com.labplatform.domain.box.FlagKind;
import com.labplatform.domain.box.Own;
import com.labplatform.domain.scoring.Handle;
import com.labplatform.domain.scoring.PlayerProgress;
import com.labplatform.domain.scoring.PlayerScore;
import com.labplatform.domain.scoring.Rank;
import com.labplatform.domain.user.Actor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Progression personnelle et classement public.
 * <p>
 * Le rang se mesure en part du catalogue possédée : c'est le catalogue du
 * moment qui sert de référence, pas un total figé.
 */
public class ScoreboardService implements GetPlayerProgressUseCase, GetLeaderboardUseCase {

    private static final int MAX_LEADERBOARD_SIZE = 100;

    private final BoxRepositoryPort boxes;
    private final OwnRepositoryPort owns;

    public ScoreboardService(BoxRepositoryPort boxes, OwnRepositoryPort owns) {
        this.boxes = boxes;
        this.owns = owns;
    }

    @Override
    public PlayerProgress progressOf(Actor actor) {
        List<Box> catalogue = boxes.findAll();
        List<Own> mine = owns.findByUser(actor.userId());

        int availablePoints = catalogue.stream().mapToInt(Box::totalPoints).sum();
        int points = mine.stream().mapToInt(Own::points).sum();
        int firstBloods = (int) mine.stream().filter(Own::firstBlood).count();

        Map<Long, Set<FlagKind>> byBox = mine.stream()
                .collect(Collectors.groupingBy(Own::boxId, Collectors.mapping(Own::kind, Collectors.toSet())));
        int pwned = (int) byBox.values().stream()
                .filter(kinds -> kinds.containsAll(Set.of(FlagKind.USER, FlagKind.ROOT)))
                .count();

        return PlayerProgress.of(points, availablePoints, mine.size(), catalogue.size() * FlagKind.values().length,
                pwned, firstBloods);
    }

    @Override
    public List<LeaderboardEntry> leaderboard(Actor actor, int limit) {
        int size = Math.min(Math.max(limit, 1), MAX_LEADERBOARD_SIZE);
        int availablePoints = boxes.findAll().stream().mapToInt(Box::totalPoints).sum();

        List<PlayerScore> scores = owns.topScores(size);
        List<LeaderboardEntry> entries = new ArrayList<>(scores.size());
        for (int i = 0; i < scores.size(); i++) {
            PlayerScore score = scores.get(i);
            double ratio = availablePoints == 0 ? 0 : (double) score.points() / availablePoints;
            entries.add(new LeaderboardEntry(i + 1, Handle.fromEmail(score.email()), score.points(),
                    score.ownedFlags(), score.firstBloods(), Rank.forCompletion(ratio),
                    score.userId().equals(actor.userId())));
        }
        return entries;
    }
}
