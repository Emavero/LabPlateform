package com.labplatform.adapter.out.persistence.repository;

import com.labplatform.adapter.out.persistence.entity.BoxInstanceJpaEntity;
import com.labplatform.domain.lab.VmStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataBoxInstanceRepository extends JpaRepository<BoxInstanceJpaEntity, Long> {

    List<BoxInstanceJpaEntity> findByUserId(Long userId);

    Optional<BoxInstanceJpaEntity> findByUserIdAndBoxId(Long userId, Long boxId);

    Optional<BoxInstanceJpaEntity> findFirstByUserIdAndStatus(Long userId, VmStatus status);
}
