package com.labplatform.application.service;

import com.labplatform.application.port.in.lab.GetVmConsoleUseCase;
import com.labplatform.application.port.in.lab.GetVmInfoUseCase;
import com.labplatform.application.port.in.lab.ListVmsUseCase;
import com.labplatform.application.port.in.lab.ProvisionDefaultLabUseCase;
import com.labplatform.application.port.in.lab.StartVmUseCase;
import com.labplatform.application.port.in.lab.StopVmUseCase;
import com.labplatform.application.port.out.HypervisorPort;
import com.labplatform.application.port.out.TransactionPort;
import com.labplatform.application.port.out.VirtualMachineRepositoryPort;
import com.labplatform.domain.lab.ConnectionInfo;
import com.labplatform.domain.lab.LabTemplate;
import com.labplatform.domain.lab.OperatingSystem;
import com.labplatform.domain.lab.VirtualMachine;
import com.labplatform.domain.lab.VmAccessPolicy;
import com.labplatform.domain.shared.NotFoundException;
import com.labplatform.domain.user.Actor;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Orchestration du lab : provisionnement, consultation, démarrage et arrêt.
 * <p>
 * Les appels à l'hyperviseur sont faits HORS transaction de base de données :
 * un démarrage réel peut prendre plusieurs secondes et ne doit pas bloquer
 * une connexion SQL pendant ce temps.
 */
public class LabService implements ListVmsUseCase, GetVmInfoUseCase, StartVmUseCase, StopVmUseCase,
        GetVmConsoleUseCase, ProvisionDefaultLabUseCase {

    private static final Comparator<VirtualMachine> DISPLAY_ORDER =
            Comparator.comparing((VirtualMachine vm) -> vm.getOperatingSystem().ordinal());

    private final VirtualMachineRepositoryPort machines;
    private final HypervisorPort hypervisor;
    private final TransactionPort transactions;
    private final Clock clock;

    public LabService(VirtualMachineRepositoryPort machines, HypervisorPort hypervisor,
                      TransactionPort transactions, Clock clock) {
        this.machines = machines;
        this.hypervisor = hypervisor;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public void provisionDefaultLab(Long userId) {
        transactions.inTransaction(() -> {
            Set<OperatingSystem> existing = machines.findByOwner(userId).stream()
                    .map(VirtualMachine::getOperatingSystem)
                    .collect(Collectors.toSet());
            LabTemplate.defaultMachines().stream()
                    .filter(os -> !existing.contains(os))
                    .forEach(os -> machines.save(VirtualMachine.provision(userId, os)));
        });
    }

    @Override
    public List<VirtualMachine> listMachines(Actor actor) {
        // Auto-réparation : un compte sans lab (créé avant ce module, par exemple) en reçoit un.
        provisionDefaultLab(actor.userId());
        return machines.findByOwner(actor.userId()).stream().sorted(DISPLAY_ORDER).toList();
    }

    @Override
    public VirtualMachine getMachine(Actor actor, Long vmId) {
        return requireAccessible(actor, vmId);
    }

    @Override
    public VirtualMachine start(Actor actor, Long vmId) {
        VirtualMachine vm = requireAccessible(actor, vmId);
        vm.ensureCanStart();

        ConnectionInfo connection = hypervisor.powerOn(vm);

        return transactions.inTransaction(() -> {
            VirtualMachine current = requireAccessible(actor, vmId);
            current.markStarted(connection, clock.instant());
            return machines.save(current);
        });
    }

    @Override
    public VirtualMachine stop(Actor actor, Long vmId) {
        VirtualMachine vm = requireAccessible(actor, vmId);
        vm.ensureCanStop();

        hypervisor.powerOff(vm);

        return transactions.inTransaction(() -> {
            VirtualMachine current = requireAccessible(actor, vmId);
            current.markStopped();
            return machines.save(current);
        });
    }

    @Override
    public List<String> consoleLog(Actor actor, Long vmId) {
        return hypervisor.consoleLog(requireAccessible(actor, vmId));
    }

    private VirtualMachine requireAccessible(Actor actor, Long vmId) {
        VirtualMachine vm = machines.findById(vmId).orElseThrow(() -> new NotFoundException("Machine introuvable"));
        return VmAccessPolicy.requireAccess(actor, vm);
    }
}
