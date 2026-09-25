package com.labplatform.domain.box;

/**
 * Difficulté d'une machine du catalogue. Elle fixe à elle seule le barème :
 * ajouter un palier revient à ajouter une constante ici, aucun cas d'usage
 * ne connaît de nombre de points en dur.
 * <p>
 * Le flag utilisateur vaut 40 % du total, le flag root les 60 % restants :
 * l'élévation de privilèges est la partie qui rapporte le plus.
 */
public enum Difficulty {

    VERY_EASY("Très facile", 4, 6),
    EASY("Facile", 8, 12),
    MEDIUM("Moyenne", 12, 18),
    HARD("Difficile", 16, 24),
    INSANE("Insane", 20, 30);

    private final String displayName;
    private final int userFlagPoints;
    private final int rootFlagPoints;

    Difficulty(String displayName, int userFlagPoints, int rootFlagPoints) {
        this.displayName = displayName;
        this.userFlagPoints = userFlagPoints;
        this.rootFlagPoints = rootFlagPoints;
    }

    public String displayName() {
        return displayName;
    }

    public int pointsFor(FlagKind kind) {
        return kind == FlagKind.USER ? userFlagPoints : rootFlagPoints;
    }

    /** Points d'une machine entièrement possédée (user + root). */
    public int totalPoints() {
        return userFlagPoints + rootFlagPoints;
    }
}
