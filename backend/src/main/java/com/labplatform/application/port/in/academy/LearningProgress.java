package com.labplatform.application.port.in.academy;

import com.labplatform.domain.academy.Track;

/**
 * Avancement d'un apprenant sur une filière entière.
 *
 * @param minutesDone durée cumulée des sections terminées
 */
public record LearningProgress(Track track, int courses, int coursesCompleted, int sections, int sectionsCompleted,
                               int minutesDone) {

    public double ratio() {
        return sections == 0 ? 0 : (double) sectionsCompleted / sections;
    }
}
