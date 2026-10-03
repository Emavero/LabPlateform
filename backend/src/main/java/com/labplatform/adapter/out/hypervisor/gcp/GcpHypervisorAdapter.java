package com.labplatform.adapter.out.hypervisor.gcp;

import com.labplatform.adapter.out.gcp.ComputeInstances;
import com.labplatform.adapter.out.gcp.GcpSettings;
import com.labplatform.adapter.out.hypervisor.SimulatedHypervisorAdapter;
import com.labplatform.application.port.out.HypervisorPort;
import com.labplatform.config.AppProperties;
import com.labplatform.domain.box.Box;
import com.labplatform.domain.lab.ConnectionInfo;
import com.labplatform.domain.lab.MachineState;
import com.labplatform.domain.lab.VirtualMachine;
import com.labplatform.domain.shared.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mode app.hypervisor.mode=gcp : les cibles du catalogue sont de vraies
 * instances Compute Engine.
 * <p>
 * Le nom de l'instance se déduit de l'identifiant de la machine par le gabarit
 * app.gcp.target-name, comme l'image d'une cible Docker se déduit du sien :
 * quelle instance héberge quelle machine est un détail de déploiement, pas une
 * donnée du catalogue.
 * <p>
 * Les machines d'attaque restent simulées. Elles sont personnelles — une par
 * joueur, créée et détruite à la demande — alors qu'une instance Compute Engine
 * préexiste et se partage : les allumer toutes reviendrait à provisionner
 * autant de machines qu'il y a d'inscrits. Brancher un vrai provisionnement
 * pour elles demandera un adaptateur de plus, pas une retouche de celui-ci.
 * <p>
 * Une instance est partagée entre les joueurs : l'identifiant de l'utilisateur
 * ne sert donc qu'à la journalisation, pas à désigner une machine. C'est une
 * limite assumée du modèle « une instance par cible », et elle est documentée
 * dans docs/GCP.md.
 */
@Component
@ConditionalOnProperty(prefix = "app.hypervisor", name = "mode", havingValue = "gcp")
public class GcpHypervisorAdapter implements HypervisorPort {

    private static final Logger log = LoggerFactory.getLogger(GcpHypervisorAdapter.class);

    private final ComputeInstances instances;
    private final GcpSettings settings;
    private final HypervisorPort attackMachines;

    public GcpHypervisorAdapter(ComputeInstances instances, GcpSettings settings, AppProperties properties) {
        this.instances = instances;
        this.settings = settings;
        this.attackMachines = new SimulatedHypervisorAdapter(properties);
    }

    @Override
    public ConnectionInfo powerOn(VirtualMachine vm) {
        return attackMachines.powerOn(vm);
    }

    @Override
    public void powerOff(VirtualMachine vm) {
        attackMachines.powerOff(vm);
    }

    @Override
    public List<String> consoleLog(VirtualMachine vm) {
        return attackMachines.consoleLog(vm);
    }

    @Override
    public String powerOnTarget(Box box, Long userId) {
        String instance = settings.targetInstanceName(box.getSlug());
        MachineState state = instances.describe(instance);
        if (state.status().isTransitional()) {
            throw new ConflictException("La machine change d'état, attendez la fin de l'opération en cours");
        }
        // Déjà allumée par quelqu'un d'autre : on rend son adresse plutôt que de
        // refuser. Une cible se partage, et elle est prête — refuser ici
        // n'apprendrait rien à celui qui arrive en second.
        MachineState ready = state.status().isRunning() ? state : instances.start(instance);
        log.info("Cible {} sur l'instance {} demandée par l'utilisateur {} : {}",
                box.getSlug(), instance, userId, ready.status());
        return ready.address().orElseThrow(() -> new ConflictException(
                "La machine démarre, son adresse sera disponible dans quelques instants"));
    }

    @Override
    public void powerOffTarget(Box box, Long userId) {
        String instance = settings.targetInstanceName(box.getSlug());
        MachineState state = instances.describe(instance);
        if (state.status().isStopped() || state.status().isTransitional()) {
            // Déjà éteinte, ou en train de l'être : il n'y a rien à demander.
            return;
        }
        instances.stop(instance);
        log.info("Cible {} sur l'instance {} arrêtée à la demande de l'utilisateur {}",
                box.getSlug(), instance, userId);
    }
}
