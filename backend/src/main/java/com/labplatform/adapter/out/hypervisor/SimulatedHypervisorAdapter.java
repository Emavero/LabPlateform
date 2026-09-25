package com.labplatform.adapter.out.hypervisor;

import com.labplatform.application.port.out.HypervisorPort;
import com.labplatform.config.AppProperties;
import com.labplatform.domain.lab.AccessProtocol;
import com.labplatform.domain.lab.ConnectionInfo;
import com.labplatform.domain.lab.OperatingSystem;
import com.labplatform.domain.lab.VirtualMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;

/**
 * Hyperviseur simulé : aucune machine n'est réellement créée. Il attribue une
 * adresse et des identifiants temporaires crédibles, avec un délai de
 * démarrage configurable pour que l'interface reflète un vrai démarrage.
 * <p>
 * Pour brancher une vraie infrastructure (Proxmox, vSphere, libvirt, cloud...),
 * écrire un autre adaptateur implémentant HypervisorPort et le sélectionner
 * avec app.hypervisor.mode. Aucun cas d'usage n'est à modifier.
 */
@Component
@ConditionalOnProperty(prefix = "app.hypervisor", name = "mode", havingValue = "simulated", matchIfMissing = true)
public class SimulatedHypervisorAdapter implements HypervisorPort {

    private static final Logger log = LoggerFactory.getLogger(SimulatedHypervisorAdapter.class);
    private final SecureRandom random = new SecureRandom();
    private final TemporaryPasswordGenerator passwords = new TemporaryPasswordGenerator();
    private final AppProperties.Hypervisor settings;

    public SimulatedHypervisorAdapter(AppProperties properties) {
        this.settings = properties.getHypervisor();
    }

    @Override
    public ConnectionInfo powerOn(VirtualMachine vm) {
        simulateLatency(settings.getSimulatedBootDelay());
        AccessProtocol protocol = vm.getOperatingSystem().protocol();
        ConnectionInfo info = new ConnectionInfo(
                randomHost(),
                protocol.defaultPort(),
                protocol,
                settings.getDefaultUsername(),
                passwords.next());
        log.info("[simulation] VM {} ({}) démarrée : {}", vm.getId(), vm.getOperatingSystem(), info);
        return info;
    }

    @Override
    public void powerOff(VirtualMachine vm) {
        simulateLatency(settings.getSimulatedShutdownDelay());
        log.info("[simulation] VM {} ({}) arrêtée", vm.getId(), vm.getOperatingSystem());
    }

    @Override
    public List<String> consoleLog(VirtualMachine vm) {
        if (!vm.isRunning()) {
            return List.of("[info] Machine arrêtée — aucun journal disponible.");
        }
        ConnectionInfo c = vm.getConnection().orElseThrow();
        String service = vm.getOperatingSystem() == OperatingSystem.WINDOWS ? "Remote Desktop Services" : "OpenSSH server";
        return List.of(
                "[boot] Démarrage de " + vm.getOperatingSystem().displayName() + "…",
                "[net]  Adresse IP attribuée : " + c.host(),
                "[svc]  " + service + " en écoute sur le port " + c.port(),
                "[auth] Compte temporaire « " + c.username() + " » activé",
                "[ok]   Machine prête");
    }

    private String randomHost() {
        return settings.getSimulatedSubnetPrefix() + "." + (1 + random.nextInt(254)) + "." + (2 + random.nextInt(252));
    }

    private static void simulateLatency(Duration delay) {
        if (delay == null || delay.isZero() || delay.isNegative()) {
            return;
        }
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
