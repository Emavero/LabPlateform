package com.labplatform.application.port.in.academy;

import com.labplatform.domain.user.Actor;

public interface GetCourseUseCase {

    CourseView getCourse(Actor actor, String slug);
}
