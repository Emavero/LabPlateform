package com.labplatform.application.fakes;

import com.labplatform.application.port.out.VirtualMachineRepositoryPort;
import com.labplatform.domain.lab.VirtualMachine;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryMachines implements VirtualMachineRepositoryPort {

    private final Map<Long, VirtualMachine> store = new LinkedHashMap<>();
    private long sequence = 0;

    @Override
    public List<VirtualMachine> findByOwner(Long ownerId) {
        return store.values().stream().filter(vm -> vm.isOwnedBy(ownerId)).map(this::copy).toList();
    }

    @Override
    public Optional<VirtualMachine> findById(Long id) {
        return Optional.ofNullable(store.get(id)).map(this::copy);
    }

    @Override
    public VirtualMachine save(VirtualMachine vm) {
        Long id = vm.getId() != null ? vm.getId() : ++sequence;
        VirtualMachine stored = VirtualMachine.restore(id, vm.getOwnerId(), vm.getOperatingSystem(), vm.getStatus(),
                vm.getConnection().orElse(null), vm.getStartedAt().orElse(null));
        store.put(id, stored);
        return copy(stored);
    }

    public int count() {
        return store.size();
    }

    /** Copie défensive : se comporte comme une vraie base (pas de partage d'instance). */
    private VirtualMachine copy(VirtualMachine vm) {
        return VirtualMachine.restore(vm.getId(), vm.getOwnerId(), vm.getOperatingSystem(), vm.getStatus(),
                vm.getConnection().orElse(null), vm.getStartedAt().orElse(null));
    }
}
