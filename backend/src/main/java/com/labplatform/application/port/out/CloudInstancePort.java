package com.labplatform.application.port.out;

import com.labplatform.domain.lab.MachineState;

/**
 * Connecteur vers la machine cible unique de la plateforme, celle que le
 * bouton « Démarrer / Arrêter » pilote.
 * <p>
 * Distinct de {@link HypervisorPort}, qui provisionne une machine par joueur :
 * ici l'instance est fixée par la configuration, partagée, et il n'y a rien à
 * créer ni à détruire — seulement à allumer et à éteindre.
 * <p>
 * Chaque méthode rend l'état observé après l'ordre, et non un accusé de
 * réception : c'est cet état que l'interface affiche, et c'est lui qui dit s'il
 * faut redemander.
 */
public interface CloudInstancePort {

    /** Demande le démarrage. L'état rendu est généralement transitoire. */
    MachineState start();

    MachineState stop();

    MachineState state();
}
