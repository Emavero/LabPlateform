package com.labplatform.application.port.out;

import com.labplatform.domain.billing.Subscription;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepositoryPort {

    /** Vide pour un compte qui n'a jamais payé : c'est un compte gratuit. */
    Optional<Subscription> findByUser(Long userId);

    Subscription save(Subscription subscription);

    /** Abonnements encore valides à cet instant, pour les indicateurs. */
    List<Subscription> findActiveAt(Instant now);
}
