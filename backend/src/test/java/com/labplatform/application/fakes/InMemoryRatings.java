package com.labplatform.application.fakes;

import com.labplatform.application.port.out.BoxRatingRepositoryPort;
import com.labplatform.domain.box.BoxRating;
import com.labplatform.domain.box.Difficulty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryRatings implements BoxRatingRepositoryPort {

    private final Map<String, BoxRating> store = new LinkedHashMap<>();
    private long sequence = 0;

    @Override
    public Optional<BoxRating> find(Long userId, Long boxId) {
        return Optional.ofNullable(store.get(key(userId, boxId)));
    }

    /** Un seul vote par (joueur, machine), comme la contrainte d'unicité en base. */
    @Override
    public BoxRating save(BoxRating rating) {
        String key = key(rating.userId(), rating.boxId());
        Long id = store.containsKey(key) ? store.get(key).id() : ++sequence;
        BoxRating stored = BoxRating.restore(id, rating.userId(), rating.boxId(), rating.difficulty(),
                rating.ratedAt());
        store.put(key, stored);
        return stored;
    }

    @Override
    public List<Tally> tallies() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        store.values().forEach(rating ->
                counts.merge(rating.boxId() + "/" + rating.difficulty().name(), 1, Integer::sum));
        List<Tally> tallies = new ArrayList<>();
        counts.forEach((key, votes) -> {
            String[] parts = key.split("/");
            tallies.add(new Tally(Long.valueOf(parts[0]), Difficulty.valueOf(parts[1]), votes));
        });
        return tallies;
    }

    private static String key(Long userId, Long boxId) {
        return userId + ":" + boxId;
    }
}
