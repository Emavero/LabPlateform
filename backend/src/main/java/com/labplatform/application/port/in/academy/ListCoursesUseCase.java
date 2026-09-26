package com.labplatform.application.port.in.academy;

import com.labplatform.domain.academy.Track;
import com.labplatform.domain.user.Actor;

import java.util.List;

public interface ListCoursesUseCase {

    /** Cours d'une filière, ou tout le catalogue si la filière est nulle. */
    List<CourseView> listCourses(Actor actor, Track track);
}
