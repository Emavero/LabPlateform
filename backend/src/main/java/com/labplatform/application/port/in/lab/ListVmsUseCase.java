package com.labplatform.application.port.in.lab;

import com.labplatform.domain.lab.VirtualMachine;
import com.labplatform.domain.user.Actor;

import java.util.List;

public interface ListVmsUseCase {

    List<VirtualMachine> listMachines(Actor actor);
}
