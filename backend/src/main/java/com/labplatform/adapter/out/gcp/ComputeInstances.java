package com.labplatform.adapter.out.gcp;

import com.labplatform.domain.lab.MachineState;

import java.util.Optional;

/**
 * Les gestes dont la plateforme a besoin sur une instance Compute Engine.
 * <p>
 * Cette couture existe pour la même raison que {@code CommandRunner} du côté
 * Docker : elle isole l'appel réseau, de sorte que la logique d'état se teste
 * sans projet GCP ni identifiants.
 * <p>
 * Les trois premiers gestes suffisent à une instance qui préexiste, celle que
 * pilote le bouton unique. Les trois suivants servent au modèle « une instance
 * par apprenant », où la machine est créée à la demande puis détruite : la
 * détruire plutôt que l'éteindre évite de payer le disque d'une cible que
 * personne n'attaque plus.
 */
public interface ComputeInstances {

    MachineState describe(String instanceName);

    /** Demande le démarrage et rend l'état observé juste après. */
    MachineState start(String instanceName);

    MachineState stop(String instanceName);

    /**
     * État de l'instance, ou rien si elle n'existe pas.
     * <p>
     * Distinct de {@link #describe(String)}, qui tient l'absence pour une
     * erreur : ici, « elle n'existe pas encore » est la réponse normale avant
     * de créer la cible d'un apprenant, pas une anomalie à signaler.
     */
    Optional<MachineState> find(String instanceName);

    /** Crée l'instance, attend qu'elle existe, et rend l'état observé. */
    MachineState create(String instanceName, TargetBlueprint blueprint);

    /** Détruit l'instance. Une instance déjà absente n'est pas une erreur. */
    void delete(String instanceName);
}
