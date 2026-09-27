package com.labplatform.domain.insight;

/**
 * Ce que l'administrateur doit faire de la recommandation.
 * <p>
 * Trois niveaux suffisent : quelque chose ne va pas, quelque chose est à
 * gagner, ou c'est simplement bon à savoir. Au-delà, personne ne fait plus la
 * différence entre les niveaux, et tout devient urgent.
 */
public enum RecommendationSeverity {

    /** Un problème constaté : des comptes butent ou s'en vont. */
    WARNING("À corriger"),
    /** Une demande visible qui n'est pas encore servie. */
    OPPORTUNITY("À saisir"),
    /** Un constat utile, sans action immédiate. */
    INFO("Bon à savoir");

    private final String displayName;

    RecommendationSeverity(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
