package com.labplatform.adapter.out.persistence;

import com.labplatform.adapter.out.persistence.entity.SubscriptionJpaEntity;
import com.labplatform.adapter.out.persistence.repository.SpringDataSubscriptionRepository;
import com.labplatform.application.port.out.SubscriptionRepositoryPort;
import com.labplatform.domain.billing.Subscription;
import com.labplatform.domain.billing.SubscriptionStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class SubscriptionPersistenceAdapter implements SubscriptionRepositoryPort {

    private final SpringDataSubscriptionRepository repository;

    public SubscriptionPersistenceAdapter(SpringDataSubscriptionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Subscription> findByUser(Long userId) {
        return repository.findByUserId(userId).map(SubscriptionPersistenceAdapter::toDomain);
    }

    @Override
    public Subscription save(Subscription subscription) {
        // Une seule ligne par compte : un abonnement sans identifiant peut
        // pourtant en avoir une (compte ayant déjà payé puis laissé expirer).
        Long id = subscription.getId() != null
                ? subscription.getId()
                : repository.findByUserId(subscription.getUserId())
                        .map(SubscriptionJpaEntity::getId)
                        .orElse(null);
        return toDomain(repository.save(new SubscriptionJpaEntity(id, subscription.getUserId(),
                subscription.getPlan(), subscription.getStatus(), subscription.getPeriod(),
                subscription.getStartedAt(), subscription.getExpiresAt())));
    }

    /** Résilié mais non échu compris : l'accès court jusqu'à l'échéance payée. */
    @Override
    public List<Subscription> findActiveAt(Instant now) {
        return repository
                .findByStatusInAndExpiresAtAfter(
                        List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELLED), now)
                .stream()
                .map(SubscriptionPersistenceAdapter::toDomain)
                .toList();
    }

    private static Subscription toDomain(SubscriptionJpaEntity e) {
        return Subscription.restore(e.getId(), e.getUserId(), e.getPlan(), e.getStatus(), e.getPeriod(),
                e.getStartedAt(), e.getExpiresAt());
    }
}
