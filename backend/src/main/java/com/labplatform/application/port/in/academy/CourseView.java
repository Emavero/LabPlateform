package com.labplatform.application.port.in.academy;

import com.labplatform.domain.academy.Course;
import com.labplatform.domain.academy.CourseProgress;
import com.labplatform.domain.academy.CourseSection;
import com.labplatform.domain.academy.Quiz;

import java.util.Map;
import java.util.Set;

/**
 * Un cours vu par un apprenant : le cours, ce qu'il en a déjà terminé, et
 * les quiz de ses sections.
 */
public record CourseView(Course course, CourseProgress progress, Set<Long> completedSectionIds,
                         Map<Long, Quiz> quizzes) {

    public boolean isCompleted(CourseSection section) {
        return completedSectionIds.contains(section.id());
    }

    /** Quiz de cette section, vide si elle n'en comporte pas. */
    public Quiz quizOf(CourseSection section) {
        return quizzes.getOrDefault(section.id(), Quiz.empty(section.id()));
    }
}
