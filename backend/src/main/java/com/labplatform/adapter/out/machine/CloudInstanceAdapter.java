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
 * <p>
 * L'adresse annoncée peut différer de celle que l'hébergeur rapporte. Compute
 * Engine rend l'adresse de la carte réseau dans le VPC (10.128.0.2) ; or
 * l'apprenant arrive par un tunnel, et ne joint la machine qu'à l'adresse que
 * ce tunnel route — souvent celle du serveur VPN lui-même (10.8.0.1) quand la
 * passerelle et la cible sont la même machine. Afficher l'adresse du VPC
 * donnerait alors une adresse qui ne répond pas : le bouton semblerait marcher,
 * et rien ne serait joignable. Seul l'exploitant sait laquelle des deux
 * compte ; il la pose dans la configuration.
 */
public class CloudInstanceAdapter implements CloudInstancePort {

    private final ComputeInstances instances;
    private final String instanceName;
    private final String advertisedAddress;

    public CloudInstanceAdapter(ComputeInstances instances, String instanceName) {
        this(instances, instanceName, null);
    }

    /**
     * @param advertisedAddress adresse montrée à l'apprenant, en remplacement de
     *                          celle que l'hébergeur rapporte ; vide pour garder
     *                          celle de l'hébergeur
     */
    public CloudInstanceAdapter(ComputeInstances instances, String instanceName, String advertisedAddress) {
        this.instances = instances;
        this.instanceName = instanceName;
        this.advertisedAddress = advertisedAddress == null || advertisedAddress.isBlank()
                ? null
                : advertisedAddress.trim();
    }

    @Override
    public MachineState start() {
        requireSettled(instances.describe(instanceName), true);
        return advertised(instances.start(instanceName));
    }

    @Override
    public MachineState stop() {
        requireSettled(instances.describe(instanceName), false);
        return advertised(instances.stop(instanceName));
    }

    @Override
    public MachineState state() {
        return advertised(instances.describe(instanceName));
    }

    /**
     * Remplace l'adresse rapportée par celle que l'exploitant annonce, quand il
     * en a posé une. Seule une machine en marche a une adresse à montrer : un
     * état de passage n'en porte pas, et il n'y a rien à remplacer.
     */
    private MachineState advertised(MachineState state) {
        if (advertisedAddress == null || !state.status().isRunning()) {
            return state;
        }
        return MachineState.running(advertisedAddress);
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
