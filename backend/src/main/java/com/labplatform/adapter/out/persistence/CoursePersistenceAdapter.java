package com.labplatform.adapter.out.persistence;

import com.labplatform.adapter.out.persistence.entity.CourseJpaEntity;
import com.labplatform.adapter.out.persistence.entity.CourseSectionJpaEntity;
import com.labplatform.adapter.out.persistence.repository.SpringDataCourseRepository;
import com.labplatform.adapter.out.persistence.repository.SpringDataCourseSectionRepository;
import com.labplatform.application.port.out.CourseRepositoryPort;
import com.labplatform.domain.academy.Course;
import com.labplatform.domain.academy.CourseSection;
import com.labplatform.domain.academy.Track;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Le cours et ses sections sont deux tables, mais un seul agrégat : elles
 * sont rechargées ensemble, en une requête par table plutôt qu'une par cours.
 */
@Component
public class CoursePersistenceAdapter implements CourseRepositoryPort {

    private final SpringDataCourseRepository courses;
    private final SpringDataCourseSectionRepository sections;

    public CoursePersistenceAdapter(SpringDataCourseRepository courses, SpringDataCourseSectionRepository sections) {
        this.courses = courses;
        this.sections = sections;
    }

    @Override
    public List<Course> findAll() {
        return assemble(courses.findAll());
    }

    @Override
    public List<Course> findByTrack(Track track) {
        return assemble(courses.findByTrack(track));
    }

    @Override
    public Optional<Course> findBySlug(String slug) {
        return courses.findBySlug(slug).map(entity -> assemble(List.of(entity)).get(0));
    }

    @Override
    public long count() {
        return courses.count();
    }

    @Override
    @Transactional
    public Course save(Course course) {
        CourseJpaEntity saved = courses.save(new CourseJpaEntity(course.getId(), course.getSlug(), course.getTitle(),
                course.getTrack(), course.getLevel(), course.getSummary(), course.getPublishedAt()));
        List<CourseSection> storedSections = course.getSections().stream()
                .map(section -> toDomain(sections.save(new CourseSectionJpaEntity(section.id(), saved.getId(),
                        section.slug(), section.title(), section.kind(), section.position(), section.minutes(),
                        section.content()))))
                .toList();
        return toDomain(saved, storedSections);
    }

    private List<Course> assemble(List<CourseJpaEntity> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }
        Collection<Long> ids = entities.stream().map(CourseJpaEntity::getId).toList();
        Map<Long, List<CourseSection>> sectionsByCourse = sections.findByCourseIdInOrderByPosition(ids).stream()
                .collect(Collectors.groupingBy(CourseSectionJpaEntity::getCourseId,
                        Collectors.mapping(CoursePersistenceAdapter::toDomain, Collectors.toList())));
        return entities.stream()
                .map(entity -> toDomain(entity, sectionsByCourse.getOrDefault(entity.getId(), List.of())))
                .toList();
    }

    private static Course toDomain(CourseJpaEntity e, List<CourseSection> sections) {
        return Course.restore(e.getId(), e.getSlug(), e.getTitle(), e.getTrack(), e.getLevel(), e.getSummary(),
                e.getPublishedAt(), sections);
    }

    private static CourseSection toDomain(CourseSectionJpaEntity e) {
        return new CourseSection(e.getId(), e.getSlug(), e.getTitle(), e.getKind(), e.getPosition(), e.getMinutes(),
                e.getContent());
    }
}
