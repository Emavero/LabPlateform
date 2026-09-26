package com.labplatform.application.fakes;

import com.labplatform.application.port.out.CourseRepositoryPort;
import com.labplatform.domain.academy.Course;
import com.labplatform.domain.academy.CourseSection;
import com.labplatform.domain.academy.Track;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryCourses implements CourseRepositoryPort {

    private final Map<Long, Course> store = new LinkedHashMap<>();
    private long courseSequence = 0;
    private long sectionSequence = 0;

    @Override
    public List<Course> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public List<Course> findByTrack(Track track) {
        return store.values().stream().filter(course -> course.getTrack() == track).toList();
    }

    @Override
    public Optional<Course> findBySlug(String slug) {
        return store.values().stream().filter(course -> course.getSlug().equals(slug)).findFirst();
    }

    @Override
    public long count() {
        return store.size();
    }

    /** Attribue un identifiant au cours et à chacune de ses sections, comme la base. */
    @Override
    public Course save(Course course) {
        Long id = course.getId() != null ? course.getId() : ++courseSequence;
        List<CourseSection> sections = new ArrayList<>();
        for (CourseSection section : course.getSections()) {
            Long sectionId = section.id() != null ? section.id() : ++sectionSequence;
            sections.add(new CourseSection(sectionId, section.slug(), section.title(), section.kind(),
                    section.position(), section.minutes(), section.content()));
        }
        Course stored = Course.restore(id, course.getSlug(), course.getTitle(), course.getTrack(), course.getLevel(),
                course.getSummary(), course.getPublishedAt(), sections);
        store.put(id, stored);
        return stored;
    }
}
