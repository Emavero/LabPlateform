package com.labplatform.application.port.in.lab;

import com.labplatform.domain.lab.VirtualMachine;
import com.labplatform.domain.user.Actor;

public interface StartVmUseCase {

    VirtualMachine start(Actor actor, Long vmId);
}
