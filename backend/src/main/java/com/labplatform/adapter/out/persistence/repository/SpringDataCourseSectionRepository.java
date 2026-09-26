package com.labplatform.adapter.out.persistence.repository;

import com.labplatform.adapter.out.persistence.entity.CourseSectionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SpringDataCourseSectionRepository extends JpaRepository<CourseSectionJpaEntity, Long> {

    List<CourseSectionJpaEntity> findByCourseIdInOrderByPosition(Collection<Long> courseIds);
}
