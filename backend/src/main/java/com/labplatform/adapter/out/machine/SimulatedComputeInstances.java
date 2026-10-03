package com.labplatform.adapter.out.machine;

import com.labplatform.adapter.out.gcp.ComputeInstances;

import com.labplatform.domain.lab.MachineState;
import com.labplatform.domain.lab.VmStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Cible simulée : elle traverse réellement ses états de passage.
 * <p>
 * C'est le mode par défaut, celui qui tourne sans projet GCP ni identifiants.
 * Il ne se contente pas de basculer d'un état stable à l'autre : il reste en
 * STAGING puis en STOPPING le temps configuré, ce qui permet de voir, en
 * développement, le bouton se désactiver et l'interrogation reprendre — un
 * comportement qu'un faux instantané n'aurait jamais montré.
 * <p>
 * L'horloge est injectée : l'écoulement du temps se vérifie sans attendre.
 */
public class SimulatedComputeInstances implements ComputeInstances {

    private final Clock clock;
    private final Duration bootDelay;
    private final Duration shutdownDelay;
    private final String internalIp;

    /** Instant où la transition en cours s'achève ; nul quand l'état est stable. */
    private Instant settlesAt;
    private VmStatus pending;
    private VmStatus settled = VmStatus.TERMINATED;

    public SimulatedComputeInstances(Clock clock, Duration bootDelay, Duration shutdownDelay, String internalIp) {
        this.clock = clock;
        this.bootDelay = bootDelay;
        this.shutdownDelay = shutdownDelay;
        this.internalIp = internalIp;
    }

    @Override
    public synchronized MachineState describe(String instanceName) {
        settleIfDue();
        VmStatus status = settlesAt == null ? settled : pending;
        return status.isRunning() ? MachineState.running(internalIp) : MachineState.of(status);
    }

    @Override
    public synchronized MachineState start(String instanceName) {
        settleIfDue();
        begin(VmStatus.STAGING, VmStatus.RUNNING, bootDelay);
        return describe(instanceName);
    }

    @Override
    public synchronized MachineState stop(String instanceName) {
        settleIfDue();
        begin(VmStatus.STOPPING, VmStatus.TERMINATED, shutdownDelay);
        return describe(instanceName);
    }

    private void begin(VmStatus transition, VmStatus outcome, Duration delay) {
        if (delay.isZero() || delay.isNegative()) {
            // Délai nul : les tests n'ont pas à faire avancer une horloge.
            settled = outcome;
            settlesAt = null;
            return;
        }
        pending = transition;
        settled = outcome;
        settlesAt = clock.instant().plus(delay);
    }

    private void settleIfDue() {
        if (settlesAt != null && !clock.instant().isBefore(settlesAt)) {
            settlesAt = null;
            pending = null;
        }
    }
}
