package com.labplatform.domain.academy;

import java.time.Instant;
import java.util.Objects;

/** Section cochée par un apprenant, à la date où il l'a terminée. */
public record SectionCompletion(Long id, Long userId, Long courseId, Long sectionId, Instant completedAt) {

    public SectionCompletion {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(courseId, "courseId");
        Objects.requireNonNull(sectionId, "sectionId");
        Objects.requireNonNull(completedAt, "completedAt");
    }

    public static SectionCompletion record(Long userId, Long courseId, Long sectionId, Instant completedAt) {
        return new SectionCompletion(null, userId, courseId, sectionId, completedAt);
    }

    public static SectionCompletion restore(Long id, Long userId, Long courseId, Long sectionId,
                                            Instant completedAt) {
        return new SectionCompletion(Objects.requireNonNull(id, "id"), userId, courseId, sectionId, completedAt);
    }
}
