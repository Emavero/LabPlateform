package com.labplatform.application.fakes;

import com.labplatform.application.port.out.SubscriptionRepositoryPort;
import com.labplatform.domain.billing.Subscription;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemorySubscriptions implements SubscriptionRepositoryPort {

    private final Map<Long, Subscription> byUser = new LinkedHashMap<>();
    private long sequence = 0;

    @Override
    public Optional<Subscription> findByUser(Long userId) {
        return Optional.ofNullable(byUser.get(userId));
    }

    /** Une seule ligne par compte, comme la contrainte d'unicité en base. */
    @Override
    public Subscription save(Subscription subscription) {
        Long id = subscription.getId() != null
                ? subscription.getId()
                : Optional.ofNullable(byUser.get(subscription.getUserId()))
                        .map(Subscription::getId)
                        .orElse(++sequence);
        Subscription stored = Subscription.restore(id, subscription.getUserId(), subscription.getPlan(),
                subscription.getStatus(), subscription.getPeriod(), subscription.getStartedAt(),
                subscription.getExpiresAt());
        byUser.put(stored.getUserId(), stored);
        return stored;
    }

    @Override
    public List<Subscription> findActiveAt(Instant now) {
        return byUser.values().stream().filter(subscription -> subscription.isActiveAt(now)).toList();
    }
}
