package com.labplatform.adapter.out.persistence.repository;

import com.labplatform.adapter.out.persistence.entity.VirtualMachineJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataVirtualMachineRepository extends JpaRepository<VirtualMachineJpaEntity, Long> {

    List<VirtualMachineJpaEntity> findByOwnerId(Long ownerId);
}
