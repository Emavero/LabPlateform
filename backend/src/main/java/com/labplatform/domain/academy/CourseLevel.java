package com.labplatform.domain.academy;

/** Niveau d'un cours, du premier contact à la mise en pratique avancée. */
public enum CourseLevel {

    FUNDAMENTAL("Fondamental"),
    EASY("Facile"),
    MEDIUM("Intermédiaire"),
    HARD("Avancé");

    private final String displayName;

    CourseLevel(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
