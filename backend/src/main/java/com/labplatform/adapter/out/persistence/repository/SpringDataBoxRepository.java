package com.labplatform.adapter.out.persistence.repository;

import com.labplatform.adapter.out.persistence.entity.BoxJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataBoxRepository extends JpaRepository<BoxJpaEntity, Long> {

    Optional<BoxJpaEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
