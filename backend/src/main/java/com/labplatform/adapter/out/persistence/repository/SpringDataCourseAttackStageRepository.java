package com.labplatform.adapter.out.persistence.repository;

import com.labplatform.adapter.out.persistence.entity.CourseAttackStageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface SpringDataCourseAttackStageRepository extends JpaRepository<CourseAttackStageJpaEntity, Long> {

    List<CourseAttackStageJpaEntity> findByCourseIdInOrderByPosition(Collection<Long> courseIds);

    @Transactional
    void deleteByCourseId(Long courseId);
}
