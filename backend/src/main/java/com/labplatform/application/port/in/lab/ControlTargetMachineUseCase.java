package com.labplatform.application.port.in.lab;

import com.labplatform.domain.lab.MachineState;
import com.labplatform.domain.user.Actor;

/**
 * Allumer, éteindre et observer la cible partagée de la plateforme.
 * <p>
 * Trois gestes pour une seule machine, celle que désigne la configuration.
 * L'acteur est exigé pour la même raison que partout ailleurs : une action sur
 * l'infrastructure se rattache à quelqu'un, ne serait-ce que pour la
 * journaliser.
 */
public interface ControlTargetMachineUseCase {

    MachineState start(Actor actor);

    MachineState stop(Actor actor);

    MachineState state(Actor actor);
}
