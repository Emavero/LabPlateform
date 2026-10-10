package com.labplatform.config;

import com.google.cloud.compute.v1.InstancesClient;
import com.labplatform.adapter.out.gcp.ComputeInstances;
import com.labplatform.adapter.out.gcp.GcpSettings;
import com.labplatform.adapter.out.gcp.GoogleComputeInstances;
import com.labplatform.adapter.out.machine.CloudInstanceAdapter;
import com.labplatform.adapter.out.machine.SimulatedComputeInstances;
import com.labplatform.application.port.out.CloudInstancePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Clock;

/**
 * Branchement de la cible partagée sur son hébergeur.
 * <p>
 * Deux montages pour le même port : la simulation par défaut, qui ne demande
 * rien, et Compute Engine quand app.machine.provider vaut « gcp ». Le choix est
 * fait ici et nulle part ailleurs — ni le contrôleur, ni le service ne savent
 * lequel des deux ils pilotent.
 */
@Configuration
public class GcpConfig {

    /**
     * Coordonnées du projet. Le bean n'existe qu'en mode gcp : construit, il
     * exige ses trois variables, et une configuration incomplète fait donc
     * échouer le démarrage plutôt que le premier clic.
     */
    @Bean
    @ConditionalOnGcp
    public GcpSettings gcpSettings(AppProperties properties) {
        AppProperties.Gcp gcp = properties.getGcp();
        return new GcpSettings(gcp.getProjectId(), gcp.getZone(), gcp.getInstanceName(), gcp.getTargetName(),
                gcp.getOperationTimeout());
    }

    /**
     * Client Compute Engine. Fermé à l'arrêt de l'application : il tient des
     * connexions gRPC, que laisser ouvertes retiendrait des fils d'exécution.
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnGcp
    public InstancesClient instancesClient() throws IOException {
        return InstancesClient.create();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnGcp
    public GoogleComputeInstances googleComputeInstances(InstancesClient client, GcpSettings settings) {
        return new GoogleComputeInstances(client, settings);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.machine", name = "provider", havingValue = "gcp")
    public CloudInstancePort gcpCloudInstance(ComputeInstances instances, GcpSettings settings,
                                              AppProperties properties) {
        return new CloudInstanceAdapter(instances, settings.instanceName(), properties.getMachine().getAddress());
    }

    /**
     * Cible simulée, par défaut.
     * <p>
     * Elle traverse réellement ses états de passage, en reprenant les délais du
     * mode d'hyperviseur simulé : le bouton se désactive et l'interrogation
     * reprend comme devant une vraie machine, sans projet GCP.
     */
    @Bean
    @ConditionalOnProperty(prefix = "app.machine", name = "provider", havingValue = "simulated",
            matchIfMissing = true)
    public CloudInstancePort simulatedCloudInstance(AppProperties properties, Clock clock) {
        AppProperties.Hypervisor hypervisor = properties.getHypervisor();
        ComputeInstances instances = new SimulatedComputeInstances(clock, hypervisor.getSimulatedBootDelay(),
                hypervisor.getSimulatedShutdownDelay(), properties.getMachine().getSimulatedAddress());
        return new CloudInstanceAdapter(instances, properties.getGcp().getInstanceName(),
                properties.getMachine().getAddress());
    }
}
