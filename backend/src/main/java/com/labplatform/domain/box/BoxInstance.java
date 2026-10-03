package com.labplatform.domain.box;

import com.labplatform.domain.lab.VmStatus;
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
 * <p>
 * Elle partage {@link VmStatus} avec les machines d'attaque : allumer une cible
 * ou une machine de travail, c'est le même vocabulaire, et un hébergeur qui
 * annonce « STAGING » ne distingue pas les deux. C'est déjà le cas de
 * {@code OperatingSystem}.
 */
public class BoxInstance {

    private final Long id;
    private final Long boxId;
    private final Long userId;
    private VmStatus status;
    private String address;
    private Instant startedAt;
    private Instant expiresAt;

    private BoxInstance(Long id, Long boxId, Long userId, VmStatus status, String address,
                        Instant startedAt, Instant expiresAt) {
        this.id = id;
        this.boxId = Objects.requireNonNull(boxId, "boxId");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.status = Objects.requireNonNull(status, "status");
        boolean running = status.isRunning();
        if (running != (address != null) || running != (startedAt != null) || running != (expiresAt != null)) {
            throw new IllegalStateException("Une instance démarrée a une adresse et une échéance, et elle seule");
        }
        this.address = address;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
    }

    /** Instance jamais lancée. */
    public static BoxInstance idle(Long userId, Long boxId) {
        return new BoxInstance(null, boxId, userId, VmStatus.TERMINATED, null, null, null);
    }

    public static BoxInstance restore(Long id, Long userId, Long boxId, VmStatus status, String address,
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
        this.status = VmStatus.RUNNING;
    }

    public void markStopped() {
        this.status = VmStatus.TERMINATED;
        this.address = null;
        this.startedAt = null;
        this.expiresAt = null;
    }

    /**
     * La cible est passée dans un état de passage annoncé par l'hébergeur. Son
     * adresse tombe : elle ne vaut que pour une machine réellement joignable, et
     * l'afficher pendant un démarrage ferait croire qu'on peut déjà s'y
     * connecter.
     */
    public void markTransitioning(VmStatus transitional) {
        if (!transitional.isTransitional()) {
            throw new IllegalArgumentException(transitional + " n'est pas un état de passage");
        }
        this.status = transitional;
        this.address = null;
        this.startedAt = null;
        this.expiresAt = null;
    }

    public boolean isRunning() {
        return status.isRunning();
    }

    /** La cible change d'état : il n'y a rien à lui demander pour l'instant. */
    public boolean isTransitioning() {
        return status.isTransitional();
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

    public VmStatus getStatus() {
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
