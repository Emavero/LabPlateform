package com.labplatform.application.port.out;

import com.labplatform.domain.academy.Course;
import com.labplatform.domain.academy.Track;

import java.util.List;
import java.util.Optional;

public interface CourseRepositoryPort {

    List<Course> findAll();

    List<Course> findByTrack(Track track);

    Optional<Course> findBySlug(String slug);

    long count();

    Course save(Course course);
}
