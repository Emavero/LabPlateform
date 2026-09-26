package com.labplatform.application.fakes;

import com.labplatform.application.port.out.PaymentRepositoryPort;
import com.labplatform.domain.billing.Payment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryPayments implements PaymentRepositoryPort {

    private final Map<String, Payment> byReference = new LinkedHashMap<>();
    private long sequence = 0;

    @Override
    public Payment save(Payment payment) {
        Long id = payment.getId() != null
                ? payment.getId()
                : Optional.ofNullable(byReference.get(payment.getReference()))
                        .map(Payment::getId)
                        .orElse(++sequence);
        Payment stored = Payment.restore(id, payment.getReference(), payment.getUserId(), payment.getPeriod(),
                payment.getAmount(), payment.getMethod(), payment.getStatus(),
                payment.getProviderReference().orElse(null), payment.getFailureReason().orElse(null),
                payment.getCreatedAt(), payment.getSettledAt().orElse(null));
        byReference.put(stored.getReference(), stored);
        return stored;
    }

    @Override
    public Optional<Payment> findByReference(String reference) {
        return Optional.ofNullable(byReference.get(reference));
    }

    @Override
    public List<Payment> findByUser(Long userId) {
        List<Payment> mine = new ArrayList<>(byReference.values().stream()
                .filter(payment -> payment.isOwnedBy(userId))
                .toList());
        mine.sort(Comparator.comparing(Payment::getCreatedAt).reversed());
        return mine;
    }

    @Override
    public List<Payment> findSucceededSince(Instant since) {
        return byReference.values().stream()
                .filter(Payment::hasSucceeded)
                .filter(payment -> payment.getSettledAt().map(settled -> settled.isAfter(since)).orElse(false))
                .sorted(Comparator.comparing(payment -> payment.getSettledAt().orElseThrow()))
                .toList();
    }
}
