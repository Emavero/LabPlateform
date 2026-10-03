package com.labplatform.domain.lab;

/**
 * État d'une machine, repris du vocabulaire de Compute Engine.
 * <p>
 * Les trois états transitoires ne sont pas une coquetterie : ce sont eux qui
 * permettent à l'interface de désactiver le bouton et d'interroger de nouveau,
 * plutôt que de laisser croire qu'une machine qui démarre est déjà prête. Un
 * hyperviseur qui n'en connaît pas — la simulation, Docker — passe simplement
 * d'un état stable à l'autre.
 */
public enum VmStatus {

    /** La machine est en cours de création chez l'hébergeur. */
    PROVISIONING,
    /** Les ressources sont réservées, le système démarre. */
    STAGING,
    RUNNING,
    STOPPING,
    /** Éteinte. Elle existe toujours, mais ne consomme plus. */
    TERMINATED;

    /**
     * État de passage : il changera de lui-même, sans qu'on agisse.
     * <p>
     * C'est la seule question que l'interface a besoin de poser pour décider
     * si elle doit attendre et redemander.
     */
    public boolean isTransitional() {
        return this == PROVISIONING || this == STAGING || this == STOPPING;
    }

    public boolean isRunning() {
        return this == RUNNING;
    }

    public boolean isStopped() {
        return this == TERMINATED;
    }
}
