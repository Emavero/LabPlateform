package com.labplatform.application.port.in.lab;

import com.labplatform.domain.user.Actor;

import java.util.List;

public interface GetVmConsoleUseCase {

    List<String> consoleLog(Actor actor, Long vmId);
}
