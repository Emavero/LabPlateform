package com.labplatform.application.port.in.academy;

import com.labplatform.domain.academy.Course;
import com.labplatform.domain.academy.CourseProgress;
import com.labplatform.domain.academy.CourseSection;

import java.util.Set;

/** Un cours vu par un apprenant : le cours, et ce qu'il en a déjà terminé. */
public record CourseView(Course course, CourseProgress progress, Set<Long> completedSectionIds) {

    public boolean isCompleted(CourseSection section) {
        return completedSectionIds.contains(section.id());
    }
}
