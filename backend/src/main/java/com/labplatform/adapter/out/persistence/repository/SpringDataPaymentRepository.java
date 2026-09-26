package com.labplatform.adapter.out.persistence.repository;

import com.labplatform.adapter.out.persistence.entity.PaymentJpaEntity;
import com.labplatform.domain.billing.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SpringDataPaymentRepository extends JpaRepository<PaymentJpaEntity, Long> {

    Optional<PaymentJpaEntity> findByReference(String reference);

    List<PaymentJpaEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<PaymentJpaEntity> findByStatusAndSettledAtAfterOrderBySettledAtAsc(PaymentStatus status, Instant since);
}
