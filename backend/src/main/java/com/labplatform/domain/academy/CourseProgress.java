package com.labplatform.domain.academy;

/**
 * Avancement dans un cours.
 *
 * @param completedSections sections cochées
 * @param totalSections     sections que compte le cours
 */
public record CourseProgress(int completedSections, int totalSections) {

    public CourseProgress {
        if (completedSections < 0 || totalSections < 0 || completedSections > totalSections) {
            throw new IllegalArgumentException(
                    "Avancement incohérent : " + completedSections + "/" + totalSections);
        }
    }

    /** Part du cours terminée, entre 0 et 1. */
    public double ratio() {
        return totalSections == 0 ? 0 : (double) completedSections / totalSections;
    }

    public boolean isCompleted() {
        return totalSections > 0 && completedSections == totalSections;
    }

    public boolean isStarted() {
        return completedSections > 0;
    }
}
