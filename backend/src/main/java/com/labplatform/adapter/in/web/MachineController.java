package com.labplatform.adapter.in.web;

import com.labplatform.adapter.in.web.dto.MachineDtos.MachineStateResponse;
import com.labplatform.adapter.in.web.security.AuthenticatedUser;
import com.labplatform.application.port.in.lab.ControlTargetMachineUseCase;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bouton « Démarrer / Arrêter » de la cible partagée.
 * <p>
 * Trois routes, toutes derrière la session : la chaîne de sécurité exige une
 * authentification sur tout /api/** qui n'est pas explicitement ouvert, et rien
 * ici ne l'est. Les identifiants du compte de service restent côté serveur ; le
 * client ne voit qu'un état et une adresse interne.
 */
@RestController
@RequestMapping("/api/machine")
public class MachineController {

    private final ControlTargetMachineUseCase machine;

    public MachineController(ControlTargetMachineUseCase machine) {
        this.machine = machine;
    }

    @PostMapping("/start")
    public MachineStateResponse start(@AuthenticationPrincipal AuthenticatedUser user) {
        return MachineStateResponse.from(machine.start(user.toActor()));
    }

    @PostMapping("/stop")
    public MachineStateResponse stop(@AuthenticationPrincipal AuthenticatedUser user) {
        return MachineStateResponse.from(machine.stop(user.toActor()));
    }

    @GetMapping("/status")
    public MachineStateResponse status(@AuthenticationPrincipal AuthenticatedUser user) {
        return MachineStateResponse.from(machine.state(user.toActor()));
    }
}
