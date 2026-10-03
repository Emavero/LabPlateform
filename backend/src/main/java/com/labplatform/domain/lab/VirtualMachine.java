package com.labplatform.domain.lab;

import com.labplatform.domain.shared.ConflictException;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Agrégat machine virtuelle. Porte la machine à états et garantit
 * l'invariant : des informations de connexion existent si et seulement si la
 * machine est en cours d'exécution.
 * <p>
 * Un état de passage (STAGING, STOPPING) n'est pas un état d'erreur : c'est
 * une machine dont on sait qu'elle bouge. On ne peut ni la démarrer ni
 * l'arrêter pendant ce temps — la demande précédente n'est pas finie — et elle
 * n'a pas encore, ou plus, d'informations de connexion.
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
        if (status.isRunning() != (connection != null)) {
            throw new IllegalStateException("Une VM démarrée doit avoir des informations de connexion, et elle seule");
        }
        this.connection = connection;
        this.startedAt = startedAt;
    }

    /** Nouvelle machine, éteinte, attribuée à un utilisateur. */
    public static VirtualMachine provision(Long ownerId, OperatingSystem operatingSystem) {
        return new VirtualMachine(null, ownerId, operatingSystem, VmStatus.TERMINATED, null, null);
    }

    /** Reconstitution depuis la persistance. */
    public static VirtualMachine restore(Long id, Long ownerId, OperatingSystem operatingSystem, VmStatus status,
                                         ConnectionInfo connection, Instant startedAt) {
        return new VirtualMachine(Objects.requireNonNull(id, "id"), ownerId, operatingSystem, status, connection, startedAt);
    }

    /**
     * Vérifie, sans rien modifier, qu'un ordre de démarrage peut être donné.
     * <p>
     * À ne pas confondre avec {@link #markStarted} : ceci garde l'ordre, cela
     * enregistre son résultat. Une machine qui bouge refuse l'ordre — le
     * précédent n'est pas terminé — mais accepte évidemment d'arriver au bout
     * de sa transition.
     */
    public void ensureCanStart() {
        if (status.isRunning()) {
            throw new ConflictException("La machine est déjà en cours d'exécution");
        }
        requireSettled();
    }

    /** Vérifie, sans rien modifier, qu'un ordre d'extinction peut être donné. */
    public void ensureCanStop() {
        if (status.isStopped()) {
            throw new ConflictException("La machine est déjà arrêtée");
        }
        requireSettled();
    }

    /**
     * Une machine qui bouge n'accepte pas d'ordre : le précédent n'est pas
     * terminé, et l'hébergeur refuserait de toute façon.
     */
    private void requireSettled() {
        if (status.isTransitional()) {
            throw new ConflictException("La machine change d'état, attendez la fin de l'opération en cours");
        }
    }

    /**
     * La machine est passée dans un état de transition annoncé par
     * l'hébergeur. Les informations de connexion tombent : elles ne valent que
     * pour une machine réellement joignable.
     */
    public void markTransitioning(VmStatus transitional) {
        if (!transitional.isTransitional()) {
            throw new IllegalArgumentException(transitional + " n'est pas un état de passage");
        }
        this.status = transitional;
        this.connection = null;
    }

    /** Enregistre que la machine tourne. Conclut aussi bien un ordre direct qu'une transition. */
    public void markStarted(ConnectionInfo connectionInfo, Instant now) {
        if (status.isRunning()) {
            throw new ConflictException("La machine est déjà en cours d'exécution");
        }
        Objects.requireNonNull(connectionInfo, "connectionInfo");
        if (connectionInfo.protocol() != operatingSystem.protocol()) {
            throw new IllegalArgumentException("Protocole " + connectionInfo.protocol()
                    + " incompatible avec " + operatingSystem);
        }
        this.status = VmStatus.RUNNING;
        this.connection = connectionInfo;
        this.startedAt = Objects.requireNonNull(now, "now");
    }

    /** Enregistre que la machine est éteinte. Conclut aussi bien un ordre direct qu'une transition. */
    public void markStopped() {
        if (status.isStopped()) {
            throw new ConflictException("La machine est déjà arrêtée");
        }
        this.status = VmStatus.TERMINATED;
        this.connection = null;
        this.startedAt = null;
    }

    public boolean isRunning() {
        return status.isRunning();
    }

    /** La machine change d'état : il n'y a rien à lui demander pour l'instant. */
    public boolean isTransitioning() {
        return status.isTransitional();
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
