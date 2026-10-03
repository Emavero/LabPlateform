package com.labplatform.adapter.out.gcp;

import com.google.cloud.compute.v1.Instance;
import com.google.cloud.compute.v1.InstancesClient;
import com.google.cloud.compute.v1.NetworkInterface;
import com.labplatform.domain.lab.MachineState;
import com.labplatform.domain.lab.VmStatus;
import com.labplatform.domain.shared.InvalidInputException;
import com.labplatform.domain.shared.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Accès réel à Compute Engine, par le client officiel.
 * <p>
 * Les identifiants viennent des « identifiants par défaut de l'application » :
 * la variable GOOGLE_APPLICATION_CREDENTIALS qui désigne la clé du compte de
 * service, ou l'identité attachée à la machine quand la plateforme tourne
 * elle-même sur GCP. Aucune clé n'est lue ici, et aucune ne sort du serveur.
 * <p>
 * Les ordres sont attendus jusqu'à leur terme, mais pas au-delà du délai
 * configuré : une opération qui traîne doit rendre la main avec un message
 * clair plutôt que de bloquer la requête de l'apprenant indéfiniment. L'état
 * est relu après coup, car c'est lui — et non l'accusé de réception — qui dit
 * ce qui s'est passé.
 */
public class GoogleComputeInstances implements ComputeInstances, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(GoogleComputeInstances.class);

    private final InstancesClient client;
    private final GcpSettings settings;

    public GoogleComputeInstances(InstancesClient client, GcpSettings settings) {
        this.client = client;
        this.settings = settings;
    }

    @Override
    public MachineState describe(String instanceName) {
        Instance instance = fetch(instanceName);
        String raw = instance.getStatus();
        if (!GcpStatusMapping.isKnown(raw)) {
            log.warn("État Compute Engine inconnu pour {} : « {} », traité comme un état de passage",
                    instanceName, raw);
        }
        VmStatus status = GcpStatusMapping.of(raw);
        return status.isRunning()
                ? MachineState.running(internalIpOf(instance, instanceName))
                : MachineState.of(status);
    }

    @Override
    public MachineState start(String instanceName) {
        await(instanceName, "démarrage", () -> client.startAsync(settings.projectId(), settings.zone(), instanceName)
                .get(settings.operationTimeout().toMillis(), TimeUnit.MILLISECONDS));
        return describe(instanceName);
    }

    @Override
    public MachineState stop(String instanceName) {
        await(instanceName, "extinction", () -> client.stopAsync(settings.projectId(), settings.zone(), instanceName)
                .get(settings.operationTimeout().toMillis(), TimeUnit.MILLISECONDS));
        return describe(instanceName);
    }

    @Override
    public void close() {
        client.close();
    }

    private Instance fetch(String instanceName) {
        try {
            return client.get(settings.projectId(), settings.zone(), instanceName);
        } catch (RuntimeException e) {
            // Nom inexistant, zone fausse, droits manquants : l'apprenant n'y
            // peut rien, mais le message doit désigner ce qui a été cherché.
            log.error("Lecture de l'instance {} (projet {}, zone {}) impossible",
                    instanceName, settings.projectId(), settings.zone(), e);
            throw new NotFoundException("Machine « " + instanceName + " » introuvable chez l'hébergeur");
        }
    }

    /**
     * Première adresse interne de l'instance. Une machine en marche en a
     * toujours une ; son absence est une anomalie de configuration réseau du
     * projet, pas un cas courant.
     */
    private static String internalIpOf(Instance instance, String instanceName) {
        return instance.getNetworkInterfacesList().stream()
                .map(NetworkInterface::getNetworkIP)
                .filter(ip -> ip != null && !ip.isBlank())
                .findFirst()
                .orElseThrow(() -> new InvalidInputException(
                        "La machine « " + instanceName + " » tourne sans adresse interne : vérifiez son réseau"));
    }

    private void await(String instanceName, String what, Operation operation) {
        try {
            operation.run();
        } catch (TimeoutException e) {
            throw new InvalidInputException("Le " + what + " de « " + instanceName + " » dépasse le délai prévu ("
                    + settings.operationTimeout().toSeconds() + " s). Réessayez dans un instant.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InvalidInputException("Le " + what + " de « " + instanceName + " » a été interrompu");
        } catch (ExecutionException e) {
            log.error("Échec du {} de l'instance {}", what, instanceName, e);
            throw new InvalidInputException("L'hébergeur a refusé le " + what + " de « " + instanceName + " »");
        }
    }

    /** Ordre long de Compute Engine, attendu jusqu'à son terme. */
    @FunctionalInterface
    private interface Operation {
        void run() throws InterruptedException, ExecutionException, TimeoutException;
    }
}
