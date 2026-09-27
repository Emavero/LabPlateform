package com.labplatform.domain.exposure;

/**
 * Raison qui fait monter ou baisser l'exposition d'une cible.
 * <p>
 * Un score sans ses raisons ne se conteste pas, donc ne s'améliore pas : c'est
 * pourquoi chaque cible repart avec la liste de ce qui l'a fait noter ainsi, et
 * non avec le seul chiffre.
 */
public enum ExposureSignal {

    OPEN_TO_ALL("Ouverte sans abonnement : c'est la première porte du lab"),
    LOW_DIFFICULTY("Difficulté d'entrée basse"),
    WIDELY_OWNED("Possédée par la plupart de ceux qui l'ont tentée"),
    FAST_ESCALATION("L'élévation vers root suit presque toujours l'entrée"),
    NO_RESISTANCE("Presque aucun flag refusé : rien ne freine"),
    SHARED_SEGMENT("Partage son segment réseau avec d'autres cibles"),
    REMOTE_DESKTOP("Bureau à distance exposé : surface plus large qu'un shell"),
    NEVER_BREACHED("Jamais entamée : personne n'y est encore entré"),
    RETIRED("Machine retirée du catalogue actif");

    private final String displayName;

    ExposureSignal(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
