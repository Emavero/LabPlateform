package com.labplatform.application.port.in.lab;

import com.labplatform.domain.lab.VirtualMachine;
import com.labplatform.domain.user.Actor;

public interface StopVmUseCase {

    VirtualMachine stop(Actor actor, Long vmId);
}
