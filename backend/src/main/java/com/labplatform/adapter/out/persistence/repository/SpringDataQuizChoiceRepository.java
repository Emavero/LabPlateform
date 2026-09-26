package com.labplatform.adapter.out.persistence.repository;

import com.labplatform.adapter.out.persistence.entity.QuizChoiceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface SpringDataQuizChoiceRepository extends JpaRepository<QuizChoiceJpaEntity, Long> {

    List<QuizChoiceJpaEntity> findByQuestionIdInOrderByPosition(Collection<Long> questionIds);

    @Transactional
    void deleteByQuestionIdIn(Collection<Long> questionIds);
}
