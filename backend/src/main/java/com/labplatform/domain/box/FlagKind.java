package com.labplatform.domain.box;

/**
 * Les deux flags d'une machine : celui posé dans le répertoire d'un
 * utilisateur sans privilège, puis celui que seul l'administrateur de la
 * machine peut lire.
 */
public enum FlagKind {

    USER("Flag utilisateur"),
    ROOT("Flag root");

    private final String displayName;

    FlagKind(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
