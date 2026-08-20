package com.labplatform.lab;

import com.labplatform.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/labs/vms")
@RequiredArgsConstructor
public class LabController {

    private final LabService labService;

    @GetMapping
    public List<VmResponse> list(@AuthenticationPrincipal User user) {
        return labService.listForUser(user.getId());
    }

    @GetMapping("/{id}")
    public VmResponse getOne(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return labService.getOne(user.getId(), id);
    }

    @GetMapping("/{id}/logs")
    public List<String> logs(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return labService.getLogs(user.getId(), id);
    }

    @PostMapping("/{id}/start")
    public VmResponse start(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return labService.start(user.getId(), id);
    }

    @PostMapping("/{id}/stop")
    public VmResponse stop(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return labService.stop(user.getId(), id);
    }
}
