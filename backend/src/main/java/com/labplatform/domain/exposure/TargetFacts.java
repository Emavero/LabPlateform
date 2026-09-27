package com.labplatform.domain.exposure;

import com.labplatform.domain.box.Difficulty;
import com.labplatform.domain.lab.OperatingSystem;

import java.util.Objects;

/**
 * Ce que la plateforme sait d'une cible, avant toute interprétation.
 * <p>
 * Les faits sont rassemblés par la couche applicative (catalogue, validations,
 * journal) et l'analyse ne travaille que sur eux : elle reste ainsi une
 * fonction pure, testable sans base de données, et rien de ce qu'elle conclut
 * ne peut venir d'ailleurs que de cette liste.
 *
 * @param address     adresse dans le réseau du lab
 * @param userOwns    comptes ayant validé le flag utilisateur
 * @param rootOwns    comptes ayant validé le flag root
 * @param attempts    flags refusés sur cette cible : la résistance mesurée
 * @param views       consultations de la fiche : l'intérêt qu'elle suscite
 */
public record TargetFacts(String slug, String name, OperatingSystem system, Difficulty difficulty, String address,
                          boolean retired, boolean proOnly, long userOwns, long rootOwns, long attempts, long views) {

    public TargetFacts {
        Objects.requireNonNull(slug, "slug");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(system, "system");
        Objects.requireNonNull(difficulty, "difficulty");
    }

    public ExposedService service() {
        return ExposedService.of(system);
    }

    /**
     * Segment du réseau du lab, au sens du /24 : c'est la granularité que le
     * profil VPN route, donc celle où un pivot est plausible.
     */
    public String segment() {
        if (address == null || address.isBlank()) {
            return "";
        }
        int lastDot = address.lastIndexOf('.');
        return lastDot < 0 ? address : address.substring(0, lastDot);
    }

    /** Part des comptes entrés qui sont allés jusqu'à root, de 0 à 1. */
    public double escalationRate() {
        return userOwns == 0 ? 0 : Math.min(1.0, (double) rootOwns / userOwns);
    }

    /** Refus par validation obtenue : au-dessus de 1, la cible résiste. */
    public double resistance() {
        long owned = userOwns + rootOwns;
        return owned == 0 ? attempts : (double) attempts / owned;
    }
}
