package com.labplatform.adapter.out.persistence.repository;

import com.labplatform.adapter.out.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, Long> {

    Optional<UserJpaEntity> findByEmail(String email);

    Optional<UserJpaEntity> findByResetTokenHash(String resetTokenHash);

    boolean existsByEmail(String email);
}
