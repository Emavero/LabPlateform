package com.labplatform.domain.insight;

import java.util.List;
import java.util.Map;

/**
 * Les chiffres bruts sur lesquels les recommandations sont établies, sur une
 * fenêtre d'observation donnée.
 * <p>
 * Ce type existe pour que les règles restent une fonction : on lui donne des
 * nombres, elle rend des conseils, et un test peut donc les éprouver sans base
 * de données ni horloge.
 *
 * @param lockedOutBoxes machines réservées atteintes par des comptes gratuits
 * @param stuckBoxes     machines classées par nombre de flags refusés
 * @param failedQuizzes  sections classées par nombre de quiz manqués
 * @param viewedCourses  cours classés par nombre de consultations
 * @param trackViews     consultations par filière
 * @param profiles       nombre de comptes par profil d'usage
 */
public record Signals(long users, long activeUsers, long proUsers, long checkoutsStarted, long subscriptionsStarted,
                      long paymentsFailed, long flagsValidated, long flagsRefused, long quizPassed, long quizFailed,
                      long targetsSpawned, long boxesPublished, long coursesPublished,
                      List<Count> lockedOutBoxes, List<Count> stuckBoxes, List<Count> failedQuizzes,
                      List<Count> viewedCourses, Map<String, Long> trackViews,
                      Map<UsageProfile, Long> profiles) {

    /** Un décompte nommé : une machine, un cours, une filière, et son nombre. */
    public record Count(String subject, long count) {
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Construction champ par champ.
     * <p>
     * Dix-neuf composantes ne se passent pas dans le bon ordre à la main : le
     * bâtisseur nomme chaque chiffre au moment où on le donne, et fournit pour
     * les autres une valeur neutre — celle d'une plateforme dont rien n'est à
     * signaler. Un test n'a donc à décrire que la situation qu'il éprouve.
     */
    public static final class Builder {

        private long users = 50;
        private long activeUsers = 40;
        private long proUsers = 10;
        private long checkoutsStarted = 10;
        private long subscriptionsStarted = 9;
        private long paymentsFailed = 1;
        private long flagsValidated = 60;
        private long flagsRefused = 20;
        private long quizPassed = 30;
        private long quizFailed = 5;
        private long targetsSpawned = 40;
        private long boxesPublished = 8;
        private long coursesPublished = 6;
        private List<Count> lockedOutBoxes = List.of();
        private List<Count> stuckBoxes = List.of();
        private List<Count> failedQuizzes = List.of();
        private List<Count> viewedCourses = List.of();
        private Map<String, Long> trackViews = Map.of();
        private Map<UsageProfile, Long> profiles = Map.of(UsageProfile.HUNTER, 30L, UsageProfile.LEARNER, 10L);

        public Builder users(long users) {
            this.users = users;
            return this;
        }

        public Builder activeUsers(long activeUsers) {
            this.activeUsers = activeUsers;
            return this;
        }

        public Builder proUsers(long proUsers) {
            this.proUsers = proUsers;
            return this;
        }

        public Builder checkoutsStarted(long checkoutsStarted) {
            this.checkoutsStarted = checkoutsStarted;
            return this;
        }

        public Builder subscriptionsStarted(long subscriptionsStarted) {
            this.subscriptionsStarted = subscriptionsStarted;
            return this;
        }

        public Builder paymentsFailed(long paymentsFailed) {
            this.paymentsFailed = paymentsFailed;
            return this;
        }

        public Builder flagsValidated(long flagsValidated) {
            this.flagsValidated = flagsValidated;
            return this;
        }

        public Builder flagsRefused(long flagsRefused) {
            this.flagsRefused = flagsRefused;
            return this;
        }

        public Builder quizPassed(long quizPassed) {
            this.quizPassed = quizPassed;
            return this;
        }

        public Builder quizFailed(long quizFailed) {
            this.quizFailed = quizFailed;
            return this;
        }

        public Builder targetsSpawned(long targetsSpawned) {
            this.targetsSpawned = targetsSpawned;
            return this;
        }

        public Builder boxesPublished(long boxesPublished) {
            this.boxesPublished = boxesPublished;
            return this;
        }

        public Builder coursesPublished(long coursesPublished) {
            this.coursesPublished = coursesPublished;
            return this;
        }

        public Builder lockedOutBoxes(List<Count> lockedOutBoxes) {
            this.lockedOutBoxes = lockedOutBoxes;
            return this;
        }

        public Builder stuckBoxes(List<Count> stuckBoxes) {
            this.stuckBoxes = stuckBoxes;
            return this;
        }

        public Builder failedQuizzes(List<Count> failedQuizzes) {
            this.failedQuizzes = failedQuizzes;
            return this;
        }

        public Builder viewedCourses(List<Count> viewedCourses) {
            this.viewedCourses = viewedCourses;
            return this;
        }

        public Builder trackViews(Map<String, Long> trackViews) {
            this.trackViews = trackViews;
            return this;
        }

        public Builder profiles(Map<UsageProfile, Long> profiles) {
            this.profiles = profiles;
            return this;
        }

        public Signals build() {
            return new Signals(users, activeUsers, proUsers, checkoutsStarted, subscriptionsStarted, paymentsFailed,
                    flagsValidated, flagsRefused, quizPassed, quizFailed, targetsSpawned, boxesPublished,
                    coursesPublished, lockedOutBoxes, stuckBoxes, failedQuizzes, viewedCourses, trackViews,
                    profiles);
        }
    }

    public long profileCount(UsageProfile profile) {
        return profiles.getOrDefault(profile, 0L);
    }

    /** Part d'un décompte dans le total des comptes, en pourcentage entier. */
    public int shareOfUsers(long count) {
        return users == 0 ? 0 : (int) Math.round(count * 100.0 / users);
    }
}
