package com.labplatform.domain.lab;

import java.util.List;

/**
 * Composition du lab attribué à chaque utilisateur : une machine par OS
 * listé ici. Modifier l'offre par défaut ne touche aucun cas d'usage.
 */
public final class LabTemplate {

    private static final List<OperatingSystem> DEFAULT_MACHINES = List.of(OperatingSystem.WINDOWS, OperatingSystem.LINUX);

    private LabTemplate() {
    }

    public static List<OperatingSystem> defaultMachines() {
        return DEFAULT_MACHINES;
    }
}
