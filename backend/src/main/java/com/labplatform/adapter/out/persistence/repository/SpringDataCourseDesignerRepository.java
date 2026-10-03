package com.labplatform.adapter.out.persistence.repository;

import com.labplatform.adapter.out.persistence.entity.CourseDesignerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface SpringDataCourseDesignerRepository extends JpaRepository<CourseDesignerJpaEntity, Long> {

    List<CourseDesignerJpaEntity> findByCourseIdInOrderByPosition(Collection<Long> courseIds);

    @Transactional
    void deleteByCourseId(Long courseId);
}
