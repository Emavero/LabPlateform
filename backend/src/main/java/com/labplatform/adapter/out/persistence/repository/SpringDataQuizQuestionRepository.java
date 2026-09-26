package com.labplatform.adapter.out.persistence.repository;

import com.labplatform.adapter.out.persistence.entity.QuizQuestionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface SpringDataQuizQuestionRepository extends JpaRepository<QuizQuestionJpaEntity, Long> {

    List<QuizQuestionJpaEntity> findBySectionIdInOrderByPosition(Collection<Long> sectionIds);

    @Transactional
    void deleteBySectionId(Long sectionId);
}
