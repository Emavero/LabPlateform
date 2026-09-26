package com.labplatform.application.port.out;

import com.labplatform.domain.billing.Payment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentRepositoryPort {

    Payment save(Payment payment);

    Optional<Payment> findByReference(String reference);

    /** Les plus récents d'abord. */
    List<Payment> findByUser(Long userId);

    /** Paiements encaissés depuis cette date, pour le chiffre d'affaires. */
    List<Payment> findSucceededSince(Instant since);
}
