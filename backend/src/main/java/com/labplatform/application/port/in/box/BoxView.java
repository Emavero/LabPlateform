package com.labplatform.application.port.in.box;

import com.labplatform.domain.box.Box;
import com.labplatform.domain.box.FlagKind;
import com.labplatform.domain.box.Own;

import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Une machine du catalogue vue par un joueur donné : la machine, plus ce
 * qu'il en a déjà validé. Les flags ne quittent jamais l'agrégat.
 */
public record BoxView(Box box, Map<FlagKind, Own> owns) {

    public static BoxView of(Box box, List<Own> ownsOfPlayer) {
        Map<FlagKind, Own> byKind = new EnumMap<>(FlagKind.class);
        ownsOfPlayer.stream()
                .filter(own -> own.boxId().equals(box.getId()))
                .forEach(own -> byKind.put(own.kind(), own));
        return new BoxView(box, byKind);
    }

    public boolean isOwned(FlagKind kind) {
        return owns.containsKey(kind);
    }

    /** Machine possédée de bout en bout : les deux flags sont validés. */
    public boolean isPwned() {
        return isOwned(FlagKind.USER) && isOwned(FlagKind.ROOT);
    }

    public boolean hasFirstBlood() {
        return owns.values().stream().anyMatch(Own::firstBlood);
    }

    public int pointsEarned() {
        return owns.values().stream().mapToInt(Own::points).sum();
    }

    public Instant lastOwnedAt() {
        return owns.values().stream().map(Own::ownedAt).max(Comparator.naturalOrder()).orElse(null);
    }
}
