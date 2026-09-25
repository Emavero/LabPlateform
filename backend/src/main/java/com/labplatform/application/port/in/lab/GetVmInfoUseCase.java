package com.labplatform.application.port.in.lab;

import com.labplatform.domain.lab.VirtualMachine;
import com.labplatform.domain.user.Actor;

public interface GetVmInfoUseCase {

    VirtualMachine getMachine(Actor actor, Long vmId);
}
