package com.labplatform.adapter.out.persistence.repository;

import com.labplatform.adapter.out.persistence.entity.WriteupJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface SpringDataWriteupRepository extends JpaRepository<WriteupJpaEntity, Long> {

    List<WriteupJpaEntity> findByBoxId(Long boxId);

    Optional<WriteupJpaEntity> findByAuthorIdAndBoxId(Long authorId, Long boxId);

    @Transactional
    void deleteByAuthorIdAndBoxId(Long authorId, Long boxId);
}
