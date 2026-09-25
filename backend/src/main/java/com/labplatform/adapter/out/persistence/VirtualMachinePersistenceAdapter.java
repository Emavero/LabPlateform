package com.labplatform.adapter.out.persistence;

import com.labplatform.adapter.out.persistence.entity.VirtualMachineJpaEntity;
import com.labplatform.adapter.out.persistence.repository.SpringDataVirtualMachineRepository;
import com.labplatform.application.port.out.VirtualMachineRepositoryPort;
import com.labplatform.domain.lab.ConnectionInfo;
import com.labplatform.domain.lab.VirtualMachine;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class VirtualMachinePersistenceAdapter implements VirtualMachineRepositoryPort {

    private final SpringDataVirtualMachineRepository repository;

    public VirtualMachinePersistenceAdapter(SpringDataVirtualMachineRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<VirtualMachine> findByOwner(Long ownerId) {
        return repository.findByOwnerId(ownerId).stream().map(VirtualMachinePersistenceAdapter::toDomain).toList();
    }

    @Override
    public Optional<VirtualMachine> findById(Long id) {
        return repository.findById(id).map(VirtualMachinePersistenceAdapter::toDomain);
    }

    @Override
    public VirtualMachine save(VirtualMachine vm) {
        return toDomain(repository.save(toEntity(vm)));
    }

    private static VirtualMachineJpaEntity toEntity(VirtualMachine vm) {
        Optional<ConnectionInfo> c = vm.getConnection();
        return new VirtualMachineJpaEntity(
                vm.getId(),
                vm.getOwnerId(),
                vm.getOperatingSystem(),
                vm.getStatus(),
                c.map(ConnectionInfo::host).orElse(null),
                c.map(ConnectionInfo::port).orElse(null),
                c.map(ConnectionInfo::protocol).orElse(null),
                c.map(ConnectionInfo::username).orElse(null),
                c.map(ConnectionInfo::password).orElse(null),
                vm.getStartedAt().orElse(null));
    }

    private static VirtualMachine toDomain(VirtualMachineJpaEntity e) {
        ConnectionInfo connection = e.getHost() == null
                ? null
                : new ConnectionInfo(e.getHost(), e.getPort(), e.getProtocol(), e.getAccessUsername(), e.getAccessPassword());
        return VirtualMachine.restore(e.getId(), e.getOwnerId(), e.getOperatingSystem(), e.getStatus(),
                connection, e.getStartedAt());
    }
}
