package com.labplatform.application.port.in.analytics;

/**
 * Ce que les comptes font, et à quel point ils y arrivent.
 *
 * @param flagRefusalRate part des soumissions refusées : la difficulté vécue
 * @param quizPassRate    part des quiz réussis : la clarté des cours
 */
public record EngagementMetrics(long flagsValidated, long flagsRefused, long boxesPwned, long targetsSpawned,
                                long coursesViewed, long sectionsCompleted, long quizPassed, long quizFailed,
                                long writeupsPublished, int flagRefusalRate, int quizPassRate) {
}
