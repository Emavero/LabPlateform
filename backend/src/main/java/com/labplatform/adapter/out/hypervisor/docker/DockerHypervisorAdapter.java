package com.labplatform.adapter.out.hypervisor.docker;

import com.labplatform.adapter.out.process.ProcessCommandRunner;
import com.labplatform.adapter.out.process.CommandRunner;
import com.labplatform.adapter.out.hypervisor.SimulatedHypervisorAdapter;
import com.labplatform.adapter.out.hypervisor.TemporaryPasswordGenerator;
import com.labplatform.application.port.out.HypervisorPort;
import com.labplatform.config.AppProperties;
import com.labplatform.domain.lab.ConnectionInfo;
import com.labplatform.domain.lab.OperatingSystem;
import com.labplatform.domain.lab.VirtualMachine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mode app.hypervisor.mode=docker : les machines Linux sont de vrais conteneurs
 * accessibles en SSH. Windows ne pouvant pas tourner en conteneur sur un hôte
 * Linux, la machine Windows reste simulée jusqu'au branchement d'un hyperviseur.
 */
@Component
@ConditionalOnProperty(prefix = "app.hypervisor", name = "mode", havingValue = "docker")
public class DockerHypervisorAdapter implements HypervisorPort {

    private final DockerLinuxMachines linux;
    private final HypervisorPort windows;
    private final TemporaryPasswordGenerator passwords = new TemporaryPasswordGenerator();

    public DockerHypervisorAdapter(AppProperties properties) {
        AppProperties.Hypervisor hypervisor = properties.getHypervisor();
        AppProperties.Docker docker = hypervisor.getDocker();
        DockerLabSettings settings = new DockerLabSettings(
                docker.getBinary(),
                docker.getImage(),
                docker.getPublicHost(),
                docker.getBindAddress(),
                docker.getNetwork(),
                docker.getMemory(),
                docker.getCpus(),
                docker.getPidsLimit(),
                docker.getContainerPrefix(),
                hypervisor.getDefaultUsername(),
                docker.getCommandTimeout(),
                docker.getReadyTimeout(),
                docker.getPollInterval());
        this.linux = new DockerLinuxMachines(settings, new ProcessCommandRunner());
        this.windows = new SimulatedHypervisorAdapter(properties);
    }

    @Override
    public ConnectionInfo powerOn(VirtualMachine vm) {
        return isLinux(vm) ? linux.powerOn(vm, passwords.next()) : windows.powerOn(vm);
    }

    @Override
    public void powerOff(VirtualMachine vm) {
        if (isLinux(vm)) {
            linux.powerOff(vm);
        } else {
            windows.powerOff(vm);
        }
    }

    @Override
    public List<String> consoleLog(VirtualMachine vm) {
        if (isLinux(vm)) {
            return linux.consoleLog(vm);
        }
        List<String> lines = new ArrayList<>();
        lines.add("[simulation] Machine Windows simulée : aucune VM réelle n'y est encore associée.");
        lines.addAll(windows.consoleLog(vm));
        return lines;
    }

    private static boolean isLinux(VirtualMachine vm) {
        return vm.getOperatingSystem() == OperatingSystem.LINUX;
    }
}
