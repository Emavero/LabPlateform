package com.labplatform.application.port.out;

import com.labplatform.domain.box.Box;
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

    /**
     * Démarre une cible du catalogue pour un joueur et renvoie son adresse
     * dans le réseau du lab.
     * <p>
     * Une cible n'a pas d'identifiants à renvoyer, contrairement à une
     * machine d'attaque : les obtenir est précisément l'exercice.
     */
    String powerOnTarget(Box box, Long userId);

    void powerOffTarget(Box box, Long userId);
}
