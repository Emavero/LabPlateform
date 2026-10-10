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
 * Deux modèles, selon app.gcp.target-mode :
 * <ul>
 *   <li><strong>shared</strong> : une instance préexistante par machine du
 *       catalogue, partagée entre les apprenants. L'identifiant de
 *       l'utilisateur ne sert alors qu'à la journalisation. Simple, et gratuit
 *       si l'instance tient dans la couche offerte, mais celui qui arrête la
 *       cible l'arrête pour tout le monde.</li>
 *   <li><strong>per-user</strong> : une instance par apprenant et par machine,
 *       créée au démarrage et <em>détruite</em> à l'arrêt. C'est le modèle de
 *       HackTheBox. La détruire plutôt que l'éteindre évite de payer le disque
 *       d'une cible que personne n'attaque plus — mais chaque apprenant actif
 *       est une machine facturée. Coût détaillé dans docs/GCP.md.</li>
 * </ul>
 * <p>
 * La couche application ne distingue pas les deux : elle borne déjà chaque
 * apprenant à une cible à la fois et fait expirer les instances oubliées. Tout
 * le changement tient dans cet adaptateur.
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
        return settings.perUserTargets() ? createForLearner(box, userId) : startShared(box, userId);
    }

    @Override
    public void powerOffTarget(Box box, Long userId) {
        if (settings.perUserTargets()) {
            destroyForLearner(box, userId);
        } else {
            stopShared(box, userId);
        }
    }

    /**
     * Crée la cible de cet apprenant, à lui seul.
     * <p>
     * Une instance retrouvée en marche est rendue telle quelle : c'est la même
     * demande servie deux fois — un rafraîchissement, un double clic — et la
     * détruire pour la recréer ferait perdre à l'apprenant le travail en cours.
     */
    private String createForLearner(Box box, Long userId) {
        String instance = settings.targetInstanceName(box.getSlug(), userId);
        MachineState existing = instances.find(instance).orElse(null);
        if (existing != null && existing.status().isTransitional()) {
            throw new ConflictException("La machine change d'état, attendez la fin de l'opération en cours");
        }

        MachineState ready;
        if (existing == null) {
            ready = instances.create(instance, settings.blueprintFor(box.getSlug()));
            log.info("Cible {} créée pour l'utilisateur {} sur l'instance {} : {}",
                    box.getSlug(), userId, instance, ready.status());
        } else if (existing.status().isRunning()) {
            ready = existing;
        } else {
            // Elle existe mais elle est éteinte : un arrêt précédent n'a pas pu
            // la détruire. L'allumer coûte moins qu'une recréation.
            ready = instances.start(instance);
            log.info("Cible {} rallumée pour l'utilisateur {} sur l'instance {} : {}",
                    box.getSlug(), userId, instance, ready.status());
        }
        return ready.address().orElseThrow(() -> new ConflictException(
                "La machine démarre, son adresse sera disponible dans quelques instants"));
    }

    /** Détruit la cible de cet apprenant : plus de disque, donc plus de facture. */
    private void destroyForLearner(Box box, Long userId) {
        String instance = settings.targetInstanceName(box.getSlug(), userId);
        instances.delete(instance);
        log.info("Cible {} détruite pour l'utilisateur {} (instance {})", box.getSlug(), userId, instance);
    }

    private String startShared(Box box, Long userId) {
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

    private void stopShared(Box box, Long userId) {
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
