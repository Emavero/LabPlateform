package com.labplatform.application.fakes;

import com.labplatform.application.port.out.OwnRepositoryPort;
import com.labplatform.domain.box.FlagKind;
import com.labplatform.domain.box.Own;
import com.labplatform.domain.scoring.PlayerScore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InMemoryOwns implements OwnRepositoryPort {

    private final List<Own> store = new ArrayList<>();
    private long sequence = 0;

    @Override
    public List<Own> findByUser(Long userId) {
        return store.stream().filter(own -> own.userId().equals(userId)).toList();
    }

    @Override
    public boolean exists(Long userId, Long boxId, FlagKind kind) {
        return store.stream()
                .anyMatch(own -> own.userId().equals(userId) && own.boxId().equals(boxId) && own.kind() == kind);
    }

    @Override
    public boolean noneYet(Long boxId, FlagKind kind) {
        return store.stream().noneMatch(own -> own.boxId().equals(boxId) && own.kind() == kind);
    }

    @Override
    public Own save(Own own) {
        Own stored = Own.restore(++sequence, own.userId(), own.boxId(), own.kind(), own.points(), own.firstBlood(),
                own.ownedAt());
        store.add(stored);
        return stored;
    }

    /** Même agrégation que la requête SQL : total décroissant, puis le premier arrivé. */
    @Override
    public List<PlayerScore> topScores(int limit) {
        Map<Long, List<Own>> byUser = store.stream().collect(Collectors.groupingBy(Own::userId));
        return byUser.entrySet().stream()
                .map(entry -> new PlayerScore(
                        entry.getKey(),
                        "joueur" + entry.getKey() + "@lab.test",
                        entry.getValue().stream().mapToInt(Own::points).sum(),
                        entry.getValue().size(),
                        (int) entry.getValue().stream().filter(Own::firstBlood).count(),
                        entry.getValue().stream().map(Own::ownedAt).max(Comparator.naturalOrder()).orElseThrow()))
                .sorted(Comparator.comparingInt(PlayerScore::points).reversed()
                        .thenComparing(PlayerScore::lastOwnAt))
                .limit(limit)
                .toList();
    }
}
