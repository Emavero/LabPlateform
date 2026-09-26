package com.labplatform.adapter.out.persistence.repository;

import com.labplatform.adapter.out.persistence.entity.MediaAssetJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataMediaAssetRepository extends JpaRepository<MediaAssetJpaEntity, String> {
}
