package com.labplatform.application.port.in.box;

import com.labplatform.domain.box.Box;
import com.labplatform.domain.box.BoxInstance;
import com.labplatform.domain.box.CommunityRating;
import com.labplatform.domain.box.Difficulty;
import com.labplatform.domain.box.FlagKind;
import com.labplatform.domain.box.Own;

import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Une machine du catalogue vue par un joueur donné : la machine, ce qu'il en
 * a déjà validé, et la difficulté ressentie par ceux qui l'ont faite. Les
 * flags ne quittent jamais l'agrégat.
 *
 * @param myVote   note donnée par ce joueur, nulle s'il n'a pas voté
 * @param instance cible lancée par ce joueur, nulle s'il n'en a jamais lancé
 * @param locked   machine réservée aux abonnés, que ce joueur n'est pas. La
 *                 fiche existe quand même — nom, système, difficulté, points —
 *                 pour qu'il sache ce qu'il obtiendrait ; c'est l'adaptateur
 *                 qui tait ce qui sert à l'attaquer.
 */
public record BoxView(Box box, Map<FlagKind, Own> owns, CommunityRating rating, Difficulty myVote,
                      BoxInstance instance, boolean locked) {

    public static BoxView of(Box box, List<Own> ownsOfPlayer) {
        return of(box, ownsOfPlayer, CommunityRating.NONE, null, null, false);
    }

    public static BoxView of(Box box, List<Own> ownsOfPlayer, CommunityRating rating, Difficulty myVote,
                             BoxInstance instance, boolean locked) {
        Map<FlagKind, Own> byKind = new EnumMap<>(FlagKind.class);
        ownsOfPlayer.stream()
                .filter(own -> own.boxId().equals(box.getId()))
                .forEach(own -> byKind.put(own.kind(), own));
        return new BoxView(box, byKind, rating, myVote, locked ? null : instance, locked);
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

    /** La cible du joueur tourne-t-elle ? */
    public boolean isInstanceRunning() {
        return instance != null && instance.isRunning();
    }

    public Instant lastOwnedAt() {
        return owns.values().stream().map(Own::ownedAt).max(Comparator.naturalOrder()).orElse(null);
    }
}
