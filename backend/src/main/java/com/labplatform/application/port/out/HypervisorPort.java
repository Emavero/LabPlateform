package com.labplatform.application.port.out;

import com.labplatform.domain.lab.ConnectionInfo;
import com.labplatform.domain.lab.VirtualMachine;

import java.util.List;

/**
 * Connecteur vers l'infrastructure qui héberge réellement les machines
 * (hyperviseur, cloud, conteneurs). Seul point à remplacer pour passer de
 * la simulation à un vrai provisionnement.
 */
public interface HypervisorPort {

    /** Démarre la machine et renvoie les informations d'accès qu'elle expose. */
    ConnectionInfo powerOn(VirtualMachine vm);

    void powerOff(VirtualMachine vm);

    List<String> consoleLog(VirtualMachine vm);
}
