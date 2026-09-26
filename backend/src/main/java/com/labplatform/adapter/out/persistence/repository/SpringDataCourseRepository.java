package com.labplatform.adapter.out.persistence.repository;

import com.labplatform.adapter.out.persistence.entity.CourseJpaEntity;
import com.labplatform.domain.academy.Track;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataCourseRepository extends JpaRepository<CourseJpaEntity, Long> {

    Optional<CourseJpaEntity> findBySlug(String slug);

    List<CourseJpaEntity> findByTrack(Track track);
}
