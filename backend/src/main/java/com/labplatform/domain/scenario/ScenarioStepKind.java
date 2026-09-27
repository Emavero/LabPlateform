package com.labplatform.domain.scenario;

/**
 * Nature d'une étape de scénario : ce qu'il faut faire pour la valider.
 * <p>
 * Deux natures, parce que la plateforme n'a que deux sortes de preuves qu'une
 * chose a été faite : un flag validé sur une machine, une section de cours
 * terminée. Une étape « lire la documentation » n'existe pas, faute de pouvoir
 * la constater — et une étape que l'on coche soi-même ne prouve rien.
 */
public enum ScenarioStepKind {

    MACHINE("Machine à compromettre"),
    COURSE("Cours à suivre");

    private final String displayName;

    ScenarioStepKind(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
