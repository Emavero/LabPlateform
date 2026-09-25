package com.labplatform.domain.lab;

import com.labplatform.domain.shared.ConflictException;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Agrégat machine virtuelle. Porte la machine à états (STOPPED ⇄ RUNNING)
 * et garantit l'invariant : des informations de connexion existent si et
 * seulement si la machine est en cours d'exécution.
 */
public class VirtualMachine {

    private final Long id;
    private final Long ownerId;
    private final OperatingSystem operatingSystem;
    private VmStatus status;
    private ConnectionInfo connection;
    private Instant startedAt;

    private VirtualMachine(Long id, Long ownerId, OperatingSystem operatingSystem, VmStatus status,
                           ConnectionInfo connection, Instant startedAt) {
        this.id = id;
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.operatingSystem = Objects.requireNonNull(operatingSystem, "operatingSystem");
        this.status = Objects.requireNonNull(status, "status");
        if ((status == VmStatus.RUNNING) != (connection != null)) {
            throw new IllegalStateException("Une VM démarrée doit avoir des informations de connexion, et elle seule");
        }
        this.connection = connection;
        this.startedAt = startedAt;
    }

    /** Nouvelle machine, arrêtée, attribuée à un utilisateur. */
    public static VirtualMachine provision(Long ownerId, OperatingSystem operatingSystem) {
        return new VirtualMachine(null, ownerId, operatingSystem, VmStatus.STOPPED, null, null);
    }

    /** Reconstitution depuis la persistance. */
    public static VirtualMachine restore(Long id, Long ownerId, OperatingSystem operatingSystem, VmStatus status,
                                         ConnectionInfo connection, Instant startedAt) {
        return new VirtualMachine(Objects.requireNonNull(id, "id"), ownerId, operatingSystem, status, connection, startedAt);
    }

    /** Vérifie, sans rien modifier, que la machine peut être démarrée. */
    public void ensureCanStart() {
        if (status == VmStatus.RUNNING) {
            throw new ConflictException("La machine est déjà en cours d'exécution");
        }
    }

    /** Vérifie, sans rien modifier, que la machine peut être arrêtée. */
    public void ensureCanStop() {
        if (status == VmStatus.STOPPED) {
            throw new ConflictException("La machine est déjà arrêtée");
        }
    }

    public void markStarted(ConnectionInfo connectionInfo, Instant now) {
        ensureCanStart();
        Objects.requireNonNull(connectionInfo, "connectionInfo");
        if (connectionInfo.protocol() != operatingSystem.protocol()) {
            throw new IllegalArgumentException("Protocole " + connectionInfo.protocol()
                    + " incompatible avec " + operatingSystem);
        }
        this.status = VmStatus.RUNNING;
        this.connection = connectionInfo;
        this.startedAt = Objects.requireNonNull(now, "now");
    }

    public void markStopped() {
        ensureCanStop();
        this.status = VmStatus.STOPPED;
        this.connection = null;
        this.startedAt = null;
    }

    public boolean isRunning() {
        return status == VmStatus.RUNNING;
    }

    public boolean isOwnedBy(Long userId) {
        return ownerId.equals(userId);
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public OperatingSystem getOperatingSystem() {
        return operatingSystem;
    }

    public VmStatus getStatus() {
        return status;
    }

    public Optional<ConnectionInfo> getConnection() {
        return Optional.ofNullable(connection);
    }

    public Optional<Instant> getStartedAt() {
        return Optional.ofNullable(startedAt);
    }
}
