package com.labplatform.adapter.out.persistence.repository;

import com.labplatform.adapter.out.persistence.entity.SectionCompletionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SpringDataSectionCompletionRepository extends JpaRepository<SectionCompletionJpaEntity, Long> {

    List<SectionCompletionJpaEntity> findByUserId(Long userId);

    boolean existsByUserIdAndSectionId(Long userId, Long sectionId);

    @Transactional
    void deleteByUserIdAndSectionId(Long userId, Long sectionId);
}
