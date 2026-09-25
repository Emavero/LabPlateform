package com.labplatform.application.port.out;

import com.labplatform.domain.lab.VirtualMachine;

import java.util.List;
import java.util.Optional;

public interface VirtualMachineRepositoryPort {

    List<VirtualMachine> findByOwner(Long ownerId);

    Optional<VirtualMachine> findById(Long id);

    VirtualMachine save(VirtualMachine vm);
}
