package com.labplatform.domain.lab;

import java.util.Objects;
import java.util.Optional;

/**
 * État d'une machine tel que l'hébergeur le rapporte : ce qu'elle fait, et où
 * la joindre si elle tourne.
 * <p>
 * L'adresse est interne au réseau du lab (10.x.x.x) et n'a de sens qu'une fois
 * le VPN monté. Elle n'existe que pour une machine allumée : une machine qui
 * démarre n'est pas encore joignable, et montrer son adresse ferait croire le
 * contraire.
 *
 * @param status      état courant
 * @param internalIp  adresse interne, présente si et seulement si la machine tourne
 */
public record MachineState(VmStatus status, String internalIp) {

    public MachineState {
        Objects.requireNonNull(status, "status");
        internalIp = internalIp == null || internalIp.isBlank() ? null : internalIp.trim();
        if (status.isRunning() && internalIp == null) {
            throw new IllegalArgumentException("Une machine en cours d'exécution a une adresse interne");
        }
        if (!status.isRunning() && internalIp != null) {
            throw new IllegalArgumentException("Seule une machine en cours d'exécution a une adresse interne");
        }
    }

    public static MachineState running(String internalIp) {
        return new MachineState(VmStatus.RUNNING, internalIp);
    }

    /** État sans adresse : éteinte, ou en train de changer d'état. */
    public static MachineState of(VmStatus status) {
        return new MachineState(status, null);
    }

    public Optional<String> address() {
        return Optional.ofNullable(internalIp);
    }
}
