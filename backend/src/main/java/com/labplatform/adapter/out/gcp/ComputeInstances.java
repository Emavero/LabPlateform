package com.labplatform.adapter.out.gcp;

import com.labplatform.domain.lab.MachineState;

/**
 * Les trois gestes dont la plateforme a besoin sur une instance Compute Engine.
 * <p>
 * Cette couture existe pour la même raison que {@code CommandRunner} du côté
 * Docker : elle isole l'appel réseau, de sorte que la logique d'état se teste
 * sans projet GCP ni identifiants.
 */
public interface ComputeInstances {

    MachineState describe(String instanceName);

    /** Demande le démarrage et rend l'état observé juste après. */
    MachineState start(String instanceName);

    MachineState stop(String instanceName);
}
