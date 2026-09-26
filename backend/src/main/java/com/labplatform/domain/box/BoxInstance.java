package com.labplatform.domain.box;

import com.labplatform.domain.shared.ConflictException;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Cible lancée à la demande par un joueur pour une machine du catalogue.
 * <p>
 * Une instance porte deux invariants. Une adresse et une échéance existent si
 * et seulement si elle tourne, comme pour une machine d'attaque. Et elle a une
 * durée de vie : passée l'échéance, elle est considérée comme arrêtée, ce qui
 * évite qu'une cible oubliée occupe l'infrastructure indéfiniment.
 */
public class BoxInstance {

    private final Long id;
    private final Long boxId;
    private final Long userId;
    private BoxInstanceStatus status;
    private String address;
    private Instant startedAt;
    private Instant expiresAt;

    private BoxInstance(Long id, Long boxId, Long userId, BoxInstanceStatus status, String address,
                        Instant startedAt, Instant expiresAt) {
        this.id = id;
        this.boxId = Objects.requireNonNull(boxId, "boxId");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.status = Objects.requireNonNull(status, "status");
        boolean running = status == BoxInstanceStatus.RUNNING;
        if (running != (address != null) || running != (startedAt != null) || running != (expiresAt != null)) {
            throw new IllegalStateException("Une instance démarrée a une adresse et une échéance, et elle seule");
        }
        this.address = address;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
    }

    /** Instance jamais lancée. */
    public static BoxInstance idle(Long userId, Long boxId) {
        return new BoxInstance(null, boxId, userId, BoxInstanceStatus.STOPPED, null, null, null);
    }

    public static BoxInstance restore(Long id, Long userId, Long boxId, BoxInstanceStatus status, String address,
                                      Instant startedAt, Instant expiresAt) {
        return new BoxInstance(Objects.requireNonNull(id, "id"), boxId, userId, status, address, startedAt,
                expiresAt);
    }

    public void markStarted(String address, Instant now, Duration lifetime) {
        if (isRunning()) {
            throw new ConflictException("Cette machine tourne déjà");
        }
        this.address = Objects.requireNonNull(address, "address");
        this.startedAt = Objects.requireNonNull(now, "now");
        this.expiresAt = now.plus(Objects.requireNonNull(lifetime, "lifetime"));
        this.status = BoxInstanceStatus.RUNNING;
    }

    public void markStopped() {
        this.status = BoxInstanceStatus.STOPPED;
        this.address = null;
        this.startedAt = null;
        this.expiresAt = null;
    }

    public boolean isRunning() {
        return status == BoxInstanceStatus.RUNNING;
    }

    /** Échue : elle tourne encore en base, mais sa durée de vie est dépassée. */
    public boolean isExpiredAt(Instant now) {
        return isRunning() && !now.isBefore(expiresAt);
    }

    /** Temps restant avant l'échéance, jamais négatif. */
    public Duration remainingAt(Instant now) {
        if (!isRunning()) {
            return Duration.ZERO;
        }
        Duration remaining = Duration.between(now, expiresAt);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    public Long getId() {
        return id;
    }

    public Long getBoxId() {
        return boxId;
    }

    public Long getUserId() {
        return userId;
    }

    public BoxInstanceStatus getStatus() {
        return status;
    }

    public Optional<String> getAddress() {
        return Optional.ofNullable(address);
    }

    public Optional<Instant> getStartedAt() {
        return Optional.ofNullable(startedAt);
    }

    public Optional<Instant> getExpiresAt() {
        return Optional.ofNullable(expiresAt);
    }
}
