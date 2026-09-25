package com.labplatform.domain.scoring;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankTest {

    @Test
    void climbsWithTheShareOfTheCatalogueOwned() {
        assertEquals(Rank.NOOB, Rank.forCompletion(0));
        assertEquals(Rank.NOOB, Rank.forCompletion(0.04));
        assertEquals(Rank.SCRIPT_KIDDIE, Rank.forCompletion(0.05));
        assertEquals(Rank.HACKER, Rank.forCompletion(0.20));
        assertEquals(Rank.PRO_HACKER, Rank.forCompletion(0.35));
        assertEquals(Rank.ELITE_HACKER, Rank.forCompletion(0.60));
        assertEquals(Rank.GURU, Rank.forCompletion(0.80));
        assertEquals(Rank.OMNISCIENT, Rank.forCompletion(1));
    }

    @Test
    void theLastRankHasNoNextOne() {
        assertEquals(Rank.SCRIPT_KIDDIE, Rank.NOOB.next().orElseThrow());
        assertTrue(Rank.OMNISCIENT.next().isEmpty());
    }

    @Test
    void progressTellsHowManyPointsAreLeftBeforeTheNextRank() {
        // 200 points au catalogue, 10 acquis : 5 % du contenu, donc Script Kiddie.
        PlayerProgress progress = PlayerProgress.of(10, 200, 1, 12, 0, 0);

        assertEquals(Rank.SCRIPT_KIDDIE, progress.rank());
        assertEquals(Rank.HACKER, progress.nextRank());
        assertEquals(20, progress.pointsToNextRank());
        assertEquals(0.05, progress.completion(), 1e-9);
    }

    @Test
    void anEmptyCatalogueLeavesEveryoneAtTheFirstRank() {
        PlayerProgress progress = PlayerProgress.of(0, 0, 0, 0, 0, 0);

        assertEquals(Rank.NOOB, progress.rank());
        assertEquals(0, progress.completion());
    }

    @Test
    void thePlayerWhoOwnsEverythingHasNothingLeftToEarn() {
        PlayerProgress progress = PlayerProgress.of(200, 200, 12, 12, 6, 2);

        assertEquals(Rank.OMNISCIENT, progress.rank());
        assertEquals(0, progress.pointsToNextRank());
    }
}
