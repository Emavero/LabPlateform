package com.labplatform.adapter.out.machine;

import com.labplatform.adapter.out.gcp.ComputeInstances;
import com.labplatform.adapter.out.gcp.TargetBlueprint;
import com.labplatform.domain.lab.MachineState;
import com.labplatform.domain.lab.VmStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Cibles simulées : elles traversent réellement leurs états de passage.
 * <p>
 * C'est le mode par défaut, celui qui tourne sans projet GCP ni identifiants.
 * Il ne se contente pas de basculer d'un état stable à l'autre : il reste en
 * STAGING puis en STOPPING le temps configuré, ce qui permet de voir, en
 * développement, le bouton se désactiver et l'interrogation reprendre — un
 * comportement qu'un faux instantané n'aurait jamais montré.
 * <p>
 * Chaque nom d'instance a son propre état. C'est indispensable au modèle « une
 * instance par apprenant » : avec un état unique, deux apprenants auraient
 * partagé la même machine simulée, et l'arrêt de l'un aurait éteint celle de
 * l'autre — le défaut même que ce modèle corrige.
 * <p>
 * La cible unique garde l'adresse configurée ; les instances créées à la
 * demande en dérivent une de leur nom, pour que deux cibles simulées ne se
 * présentent pas à la même adresse. Un apprenant qui voit la même adresse que
 * son voisin croirait légitimement attaquer la même machine.
 * <p>
 * L'horloge est injectée : l'écoulement du temps se vérifie sans attendre.
 */
public class SimulatedComputeInstances implements ComputeInstances {

    private final Clock clock;
    private final Duration bootDelay;
    private final Duration shutdownDelay;
    private final String internalIp;
    /** Instance dont l'adresse est celle configurée : la cible unique. */
    private final String primaryInstanceName;

    /** État par nom d'instance. Une entrée naît au premier ordre reçu. */
    private final Map<String, Simulated> byName = new HashMap<>();

    public SimulatedComputeInstances(Clock clock, Duration bootDelay, Duration shutdownDelay, String internalIp) {
        this(clock, bootDelay, shutdownDelay, internalIp, null);
    }

    public SimulatedComputeInstances(Clock clock, Duration bootDelay, Duration shutdownDelay, String internalIp,
                                     String primaryInstanceName) {
        this.clock = clock;
        this.bootDelay = bootDelay;
        this.shutdownDelay = shutdownDelay;
        this.internalIp = internalIp;
        this.primaryInstanceName = primaryInstanceName;
    }

    @Override
    public synchronized MachineState describe(String instanceName) {
        // Un nom inconnu est tenu pour une instance qui préexiste, éteinte :
        // c'est le cas de la cible unique, qu'on interroge avant tout ordre.
        // find(), lui, dit la vérité sur ce qui a été créé.
        Simulated instance = byName.get(instanceName);
        return instance == null ? MachineState.of(VmStatus.TERMINATED) : instance.state(addressOf(instanceName));
    }

    @Override
    public synchronized MachineState start(String instanceName) {
        Simulated instance = byName.computeIfAbsent(instanceName, name -> new Simulated());
        instance.begin(VmStatus.STAGING, VmStatus.RUNNING, bootDelay);
        return instance.state(addressOf(instanceName));
    }

    @Override
    public synchronized MachineState stop(String instanceName) {
        Simulated instance = byName.computeIfAbsent(instanceName, name -> new Simulated());
        instance.begin(VmStatus.STOPPING, VmStatus.TERMINATED, shutdownDelay);
        return instance.state(addressOf(instanceName));
    }

    @Override
    public synchronized Optional<MachineState> find(String instanceName) {
        Simulated instance = byName.get(instanceName);
        return Optional.ofNullable(instance).map(found -> found.state(addressOf(instanceName)));
    }

    @Override
    public synchronized MachineState create(String instanceName, TargetBlueprint blueprint) {
        // Créer puis démarrer ne fait qu'un chez Compute Engine : une instance
        // naît allumée. La simulation suit le même chemin, délai compris.
        Simulated instance = new Simulated();
        byName.put(instanceName, instance);
        instance.begin(VmStatus.PROVISIONING, VmStatus.RUNNING, bootDelay);
        return instance.state(addressOf(instanceName));
    }

    @Override
    public synchronized void delete(String instanceName) {
        byName.remove(instanceName);
    }

    /**
     * Adresse de la cible simulée. Celle configurée sert à la cible unique, dont
     * l'adresse est annoncée aux apprenants ; les instances créées à la demande
     * en dérivent une de leur nom, pour que deux cibles ne se confondent pas.
     * <p>
     * Le calcul ne dépend que du nom : une instance garde son adresse quand
     * d'autres apparaissent ou disparaissent à côté d'elle.
     */
    private String addressOf(String instanceName) {
        if (primaryInstanceName == null || primaryInstanceName.equals(instanceName)) {
            return internalIp;
        }
        int lastDot = internalIp.lastIndexOf('.');
        if (lastDot < 0) {
            return internalIp;
        }
        int host = Math.floorMod(instanceName.hashCode(), 250) + 2;
        return internalIp.substring(0, lastDot + 1) + host;
    }

    /** État d'une instance simulée : ce qui est acquis, et ce qui est en cours. */
    private final class Simulated {

        /** Instant où la transition en cours s'achève ; nul quand l'état est stable. */
        private Instant settlesAt;
        private VmStatus pending;
        private VmStatus settled = VmStatus.TERMINATED;

        private MachineState state(String address) {
            settleIfDue();
            VmStatus status = settlesAt == null ? settled : pending;
            return status.isRunning() ? MachineState.running(address) : MachineState.of(status);
        }

        private void begin(VmStatus transition, VmStatus outcome, Duration delay) {
            settleIfDue();
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
}
