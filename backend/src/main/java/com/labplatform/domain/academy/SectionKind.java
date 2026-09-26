package com.labplatform.domain.academy;

/** Nature d'une section : lire, manipuler, ou se tester. */
public enum SectionKind {

    THEORY("Cours"),
    LAB("Atelier"),
    QUIZ("Quiz");

    private final String displayName;

    SectionKind(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
