package com.labplatform.adapter.in.web;

import com.labplatform.adapter.in.web.dto.VmResponse;
import com.labplatform.adapter.in.web.security.AuthenticatedUser;
import com.labplatform.application.port.in.lab.GetVmConsoleUseCase;
import com.labplatform.application.port.in.lab.GetVmInfoUseCase;
import com.labplatform.application.port.in.lab.ListVmsUseCase;
import com.labplatform.application.port.in.lab.StartVmUseCase;
import com.labplatform.application.port.in.lab.StopVmUseCase;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/labs/vms")
public class LabController {

    private final ListVmsUseCase listVms;
    private final GetVmInfoUseCase getVmInfo;
    private final StartVmUseCase startVm;
    private final StopVmUseCase stopVm;
    private final GetVmConsoleUseCase getVmConsole;

    public LabController(ListVmsUseCase listVms, GetVmInfoUseCase getVmInfo, StartVmUseCase startVm,
                         StopVmUseCase stopVm, GetVmConsoleUseCase getVmConsole) {
        this.listVms = listVms;
        this.getVmInfo = getVmInfo;
        this.startVm = startVm;
        this.stopVm = stopVm;
        this.getVmConsole = getVmConsole;
    }

    @GetMapping
    public List<VmResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return listVms.listMachines(user.toActor()).stream().map(VmResponse::from).toList();
    }

    @GetMapping("/{id}")
    public VmResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return VmResponse.from(getVmInfo.getMachine(user.toActor(), id));
    }

    @GetMapping("/{id}/logs")
    public List<String> logs(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return getVmConsole.consoleLog(user.toActor(), id);
    }

    @PostMapping("/{id}/start")
    public VmResponse start(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return VmResponse.from(startVm.start(user.toActor(), id));
    }

    @PostMapping("/{id}/stop")
    public VmResponse stop(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return VmResponse.from(stopVm.stop(user.toActor(), id));
    }
}
