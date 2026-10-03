package com.labplatform.adapter.out.machine;

import com.labplatform.adapter.out.gcp.ComputeInstances;
import com.labplatform.application.port.out.CloudInstancePort;
import com.labplatform.domain.lab.MachineState;
import com.labplatform.domain.shared.ConflictException;

/**
 * La cible unique de la plateforme, quel que soit l'hébergeur qui la porte.
 * <p>
 * L'adaptateur relit l'état avant d'agir : démarrer une machine déjà allumée
 * n'est pas une erreur chez Compute Engine, qui ne fait rien et renvoie un
 * succès. Laisser passer cela afficherait « démarrage en cours » pour une
 * machine déjà prête — autant le dire franchement. Même raisonnement pour une
 * machine qui bouge : l'ordre précédent n'est pas fini.
 * <p>
 * Il y a là une course possible — l'état peut changer entre la lecture et
 * l'ordre — qu'on ne cherche pas à fermer : l'hébergeur reste l'autorité, et
 * cette relecture ne sert qu'à transformer son silence en message utile.
 */
public class CloudInstanceAdapter implements CloudInstancePort {

    private final ComputeInstances instances;
    private final String instanceName;

    public CloudInstanceAdapter(ComputeInstances instances, String instanceName) {
        this.instances = instances;
        this.instanceName = instanceName;
    }

    @Override
    public MachineState start() {
        requireSettled(instances.describe(instanceName), true);
        return instances.start(instanceName);
    }

    @Override
    public MachineState stop() {
        requireSettled(instances.describe(instanceName), false);
        return instances.stop(instanceName);
    }

    @Override
    public MachineState state() {
        return instances.describe(instanceName);
    }

    private static void requireSettled(MachineState current, boolean starting) {
        if (current.status().isTransitional()) {
            throw new ConflictException("La machine change d'état, attendez la fin de l'opération en cours");
        }
        if (starting && current.status().isRunning()) {
            throw new ConflictException("La machine est déjà en cours d'exécution");
        }
        if (!starting && current.status().isStopped()) {
            throw new ConflictException("La machine est déjà arrêtée");
        }
    }
}
