package com.labplatform.lab;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VmRepository extends JpaRepository<VirtualMachine, Long> {
    List<VirtualMachine> findByUserId(Long userId);
    Optional<VirtualMachine> findByIdAndUserId(Long id, Long userId);
}
