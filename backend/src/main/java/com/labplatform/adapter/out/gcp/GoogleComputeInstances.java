package com.labplatform.adapter.out.gcp;

import com.google.cloud.compute.v1.AttachedDisk;
import com.google.cloud.compute.v1.AttachedDiskInitializeParams;
import com.google.cloud.compute.v1.Instance;
import com.google.cloud.compute.v1.InstancesClient;
import com.google.cloud.compute.v1.NetworkInterface;
import com.google.cloud.compute.v1.Tags;
import com.labplatform.domain.lab.MachineState;
import com.labplatform.domain.lab.VmStatus;
import com.labplatform.domain.shared.InvalidInputException;
import com.labplatform.domain.shared.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
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
        return stateOf(fetch(instanceName), instanceName);
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
    public Optional<MachineState> find(String instanceName) {
        try {
            return Optional.of(stateOf(client.get(settings.projectId(), settings.zone(), instanceName), instanceName));
        } catch (com.google.api.gax.rpc.NotFoundException absent) {
            // L'instance n'existe pas encore : c'est la réponse attendue avant
            // de créer la cible d'un apprenant, pas une panne à signaler.
            return Optional.empty();
        } catch (RuntimeException e) {
            log.error("Lecture de l'instance {} (projet {}, zone {}) impossible",
                    instanceName, settings.projectId(), settings.zone(), e);
            throw new InvalidInputException("L'hébergeur n'a pas répondu au sujet de « " + instanceName + " »");
        }
    }

    @Override
    public MachineState create(String instanceName, TargetBlueprint blueprint) {
        Instance instance = describeFor(instanceName, blueprint);
        await(instanceName, "création", () -> client.insertAsync(settings.projectId(), settings.zone(), instance)
                .get(settings.operationTimeout().toMillis(), TimeUnit.MILLISECONDS));
        return describe(instanceName);
    }

    @Override
    public void delete(String instanceName) {
        if (find(instanceName).isEmpty()) {
            // Déjà détruite : l'ordre a abouti, même s'il a abouti avant nous.
            return;
        }
        await(instanceName, "destruction", () ->
                client.deleteAsync(settings.projectId(), settings.zone(), instanceName)
                        .get(settings.operationTimeout().toMillis(), TimeUnit.MILLISECONDS));
    }

    /**
     * Description envoyée à Compute Engine pour créer la cible.
     * <p>
     * Aucune {@code AccessConfig} n'est posée sur la carte réseau, et c'est
     * volontaire : sans elle l'instance n'a pas d'adresse externe, donc elle
     * n'est joignable que depuis le réseau du lab, par le VPN. Une cible
     * volontairement vulnérable n'a rien à faire sur Internet.
     */
    private Instance describeFor(String instanceName, TargetBlueprint blueprint) {
        AttachedDisk boot = AttachedDisk.newBuilder()
                .setBoot(true)
                .setAutoDelete(true)
                .setInitializeParams(AttachedDiskInitializeParams.newBuilder()
                        .setSourceImage(blueprint.sourceImage())
                        .setDiskSizeGb(blueprint.diskSizeGb())
                        .setDiskType(zonalResource("diskTypes", blueprint.diskType()))
                        .build())
                .build();

        NetworkInterface.Builder nic = NetworkInterface.newBuilder();
        if (!blueprint.subnetwork().isBlank()) {
            nic.setSubnetwork(blueprint.subnetwork());
        }

        Instance.Builder builder = Instance.newBuilder()
                .setName(instanceName)
                .setMachineType(zonalResource("machineTypes", blueprint.machineType()))
                .addDisks(boot)
                .addNetworkInterfaces(nic.build())
                // Repère les instances créées par la plateforme : un ménage
                // manuel ou automatique doit pouvoir les distinguer des autres.
                .putLabels("labplatform-box-target", "true");

        if (!blueprint.networkTags().isEmpty()) {
            builder.setTags(Tags.newBuilder().addAllItems(blueprint.networkTags()).build());
        }
        return builder.build();
    }

    /** Chemin d'une ressource de zone, tel que Compute Engine l'attend. */
    private String zonalResource(String kind, String name) {
        return name.startsWith("projects/") || name.startsWith("zones/")
                ? name
                : "zones/" + settings.zone() + "/" + kind + "/" + name;
    }

    private MachineState stateOf(Instance instance, String instanceName) {
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
