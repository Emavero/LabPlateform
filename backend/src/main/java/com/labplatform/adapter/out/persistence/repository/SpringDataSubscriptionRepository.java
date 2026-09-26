package com.labplatform.adapter.out.persistence.repository;

import com.labplatform.adapter.out.persistence.entity.SubscriptionJpaEntity;
import com.labplatform.domain.billing.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpringDataSubscriptionRepository extends JpaRepository<SubscriptionJpaEntity, Long> {

    Optional<SubscriptionJpaEntity> findByUserId(Long userId);

    List<SubscriptionJpaEntity> findByStatusInAndExpiresAtAfter(Collection<SubscriptionStatus> statuses, Instant now);
}
