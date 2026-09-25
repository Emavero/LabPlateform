package com.labplatform.adapter.out.persistence.repository;

import com.labplatform.adapter.out.persistence.entity.VpnProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataVpnProfileRepository extends JpaRepository<VpnProfileJpaEntity, Long> {
}
