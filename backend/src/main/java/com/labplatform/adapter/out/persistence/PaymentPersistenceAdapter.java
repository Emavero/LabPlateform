package com.labplatform.adapter.out.persistence;

import com.labplatform.adapter.out.persistence.entity.PaymentJpaEntity;
import com.labplatform.adapter.out.persistence.repository.SpringDataPaymentRepository;
import com.labplatform.application.port.out.PaymentRepositoryPort;
import com.labplatform.domain.billing.Money;
import com.labplatform.domain.billing.Payment;
import com.labplatform.domain.billing.PaymentStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class PaymentPersistenceAdapter implements PaymentRepositoryPort {

    private final SpringDataPaymentRepository repository;

    public PaymentPersistenceAdapter(SpringDataPaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Payment save(Payment payment) {
        Long id = payment.getId() != null
                ? payment.getId()
                : repository.findByReference(payment.getReference()).map(PaymentJpaEntity::getId).orElse(null);
        Money amount = payment.getAmount();
        return toDomain(repository.save(new PaymentJpaEntity(id, payment.getReference(), payment.getUserId(),
                payment.getPeriod(), amount.minorUnits(), amount.currencyCode(), payment.getMethod(),
                payment.getStatus(), payment.getProviderReference().orElse(null),
                payment.getFailureReason().orElse(null), payment.getCreatedAt(),
                payment.getSettledAt().orElse(null))));
    }

    @Override
    public Optional<Payment> findByReference(String reference) {
        return repository.findByReference(reference).map(PaymentPersistenceAdapter::toDomain);
    }

    @Override
    public List<Payment> findByUser(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(PaymentPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public List<Payment> findSucceededSince(Instant since) {
        return repository.findByStatusAndSettledAtAfterOrderBySettledAtAsc(PaymentStatus.SUCCEEDED, since).stream()
                .map(PaymentPersistenceAdapter::toDomain)
                .toList();
    }

    private static Payment toDomain(PaymentJpaEntity e) {
        return Payment.restore(e.getId(), e.getReference(), e.getUserId(), e.getPeriod(),
                new Money(e.getAmountMinor(), Money.currencyOf(e.getCurrency())), e.getMethod(), e.getStatus(),
                e.getProviderReference(), e.getFailureReason(), e.getCreatedAt(), e.getSettledAt());
    }
}
