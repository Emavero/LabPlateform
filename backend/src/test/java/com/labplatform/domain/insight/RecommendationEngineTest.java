package com.labplatform.domain.insight;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les règles sont une fonction : mêmes chiffres, mêmes conseils. Ce test les
 * épingle une par une, sans base ni horloge — c'est tout l'intérêt de les avoir
 * sorties du service.
 */
class RecommendationEngineTest {

    /** Base saine : assez de comptes, rien d'alarmant, un catalogue fourni. */
    private static Signals.Builder healthy() {
        return Signals.builder();
    }

    private static boolean has(List<Recommendation> found, String code) {
        return found.stream().anyMatch(recommendation -> recommendation.code().equals(code));
    }

    private static Recommendation get(List<Recommendation> found, String code) {
        return found.stream()
                .filter(recommendation -> recommendation.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new AssertionError("recommandation absente : " + code));
    }

    @Test
    void saysNothingUsefulBelowAHandfulOfAccounts() {
        List<Recommendation> found = RecommendationEngine.from(healthy().users(3).build());

        assertEquals(1, found.size());
        assertEquals("not-enough-data", found.get(0).code());
    }

    @Test
    void aHealthyPlatformGetsNoWarning() {
        List<Recommendation> found = RecommendationEngine.from(healthy().build());

        assertFalse(found.stream().anyMatch(r -> r.severity() == RecommendationSeverity.WARNING),
                "aucune alerte attendue : " + found);
    }

    /** Un quiz raté par la moitié des apprenants accuse la section, pas les apprenants. */
    @Test
    void pointsAtTheSectionWhoseQuizIsMissed() {
        List<Recommendation> found = RecommendationEngine.from(healthy()
                .quizPassed(4).quizFailed(12)
                .failedQuizzes(List.of(new Signals.Count("traces-disque/chronologie", 9)))
                .build());

        Recommendation quiz = get(found, "quiz-failure-rate");
        assertEquals(RecommendationSeverity.WARNING, quiz.severity());
        assertEquals("traces-disque/chronologie", quiz.subject());
        // La preuve accompagne le conseil, sans quoi il n'est pas vérifiable.
        assertTrue(quiz.evidence().contains("9"));
    }

    @Test
    void pointsAtTheMachineWhereEveryoneIsStuck() {
        List<Recommendation> found = RecommendationEngine.from(healthy()
                .flagsValidated(3).flagsRefused(40)
                .stuckBoxes(List.of(new Signals.Count("obsidian", 31)))
                .build());

        Recommendation stuck = get(found, "box-refusals");
        assertEquals("obsidian", stuck.subject());
        assertTrue(stuck.evidence().contains("31"));
    }

    @Test
    void reportsAnAbandonedCheckoutFlow() {
        List<Recommendation> found = RecommendationEngine.from(healthy()
                .checkoutsStarted(20).subscriptionsStarted(4).paymentsFailed(9)
                .build());

        Recommendation drop = get(found, "checkout-drop");
        assertEquals(RecommendationSeverity.WARNING, drop.severity());
        assertTrue(drop.evidence().contains("80 %"));
    }

    @Test
    void noticesTargetsLaunchedWithoutASingleFlag() {
        List<Recommendation> found = RecommendationEngine.from(healthy()
                .targetsSpawned(30).flagsValidated(2).flagsRefused(1)
                .build());

        assertTrue(has(found, "spawn-without-flag"));
    }

    @Test
    void warnsWhenMostAccountsNeverDidAnything() {
        Map<UsageProfile, Long> profiles = new EnumMap<>(UsageProfile.class);
        profiles.put(UsageProfile.DORMANT, 40L);
        profiles.put(UsageProfile.HUNTER, 60L);

        List<Recommendation> found = RecommendationEngine.from(healthy().users(100).profiles(profiles).build());

        assertTrue(has(found, "dormant-share"));
        assertTrue(get(found, "dormant-share").evidence().contains("40 %"));
    }

    /** Une porte fermée devant laquelle il y a du monde est une opportunité chiffrée. */
    @Test
    void turnsAPaywallHitIntoAnOpportunity() {
        List<Recommendation> found = RecommendationEngine.from(healthy()
                .lockedOutBoxes(List.of(new Signals.Count("blackice", 14)))
                .build());

        Recommendation demand = get(found, "locked-out-demand");
        assertEquals(RecommendationSeverity.OPPORTUNITY, demand.severity());
        assertEquals("blackice", demand.subject());
    }

    @Test
    void spotsAnUnderservedTrack() {
        List<Recommendation> found = RecommendationEngine.from(healthy()
                .trackViews(Map.of("Forensique", 120L, "Défense", 15L))
                .build());

        assertEquals("Forensique", get(found, "track-imbalance").subject());
    }

    @Test
    void suggestsBridgingLearnersTowardsMachines() {
        Map<UsageProfile, Long> profiles = new EnumMap<>(UsageProfile.class);
        profiles.put(UsageProfile.LEARNER, 30L);
        profiles.put(UsageProfile.HUNTER, 4L);

        List<Recommendation> found = RecommendationEngine.from(healthy().users(34).profiles(profiles).build());

        assertTrue(has(found, "learners-without-machines"));
    }

    @Test
    void mentionsAThinCatalogueWithoutRaisingAnAlarm() {
        List<Recommendation> found = RecommendationEngine.from(healthy().boxesPublished(2).coursesPublished(1).build());

        assertEquals(RecommendationSeverity.INFO, get(found, "thin-catalogue").severity());
    }

    /** Les plus graves d'abord : c'est l'ordre dans lequel on veut les lire. */
    @Test
    void ordersWarningsBeforeOpportunitiesAndFacts()  {
        List<Recommendation> found = RecommendationEngine.from(healthy()
                .checkoutsStarted(20).subscriptionsStarted(2)
                .lockedOutBoxes(List.of(new Signals.Count("blackice", 14)))
                .boxesPublished(1).coursesPublished(1)
                .build());

        List<RecommendationSeverity> order = found.stream().map(Recommendation::severity).toList();
        assertEquals(List.of(RecommendationSeverity.WARNING, RecommendationSeverity.OPPORTUNITY,
                RecommendationSeverity.INFO), order.stream().distinct().toList());
    }
}
