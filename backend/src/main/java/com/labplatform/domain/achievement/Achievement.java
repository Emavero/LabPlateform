package com.labplatform.domain.achievement;

import com.labplatform.domain.box.Difficulty;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * Hauts faits. Chacun porte sa propre condition : en ajouter un revient à
 * ajouter une constante, sans toucher au service qui les évalue.
 * <p>
 * Ils se déduisent entièrement du palmarès, donc rien n'est stocké : un haut
 * fait ne peut ni manquer, ni se perdre, ni se décerner deux fois.
 */
public enum Achievement {

    FIRST_FLAG("Premier flag", "Valider un premier flag, quel qu'il soit",
            record -> record.ownedFlags() >= 1),
    FIRST_PWN("Machine possédée", "Valider les deux flags d'une même machine",
            record -> record.boxesPwned() >= 1),
    FIVE_PWNS("Habitué", "Posséder cinq machines de bout en bout",
            record -> record.boxesPwned() >= 5),
    FIRST_BLOOD("Premier sang", "Valider un flag avant tout le monde",
            record -> record.firstBloods() >= 1),
    BLOOD_HUNTER("Chasseur de first blood", "Décrocher trois first bloods",
            record -> record.firstBloods() >= 3),
    HARD_PWN("Terrain difficile", "Posséder une machine difficile ou plus",
            record -> record.pwnedAtLeast(Difficulty.HARD)),
    INSANE_PWN("Insane", "Posséder une machine classée insane",
            record -> record.pwnedAtLeast(Difficulty.INSANE)),
    STUDENT("Élève appliqué", "Terminer une première section de cours",
            record -> record.sectionsCompleted() >= 1),
    GRADUATE("Cours terminé", "Terminer un cours en entier",
            record -> record.coursesCompleted() >= 1),
    BOTH_TRACKS("Sur les deux tableaux", "Progresser en forensique et en défense",
            record -> record.tracksStarted() >= 2);

    private final String displayName;
    private final String requirement;
    private final Predicate<PlayerRecord> rule;

    Achievement(String displayName, String requirement, Predicate<PlayerRecord> rule) {
        this.displayName = displayName;
        this.requirement = requirement;
        this.rule = rule;
    }

    public boolean isEarnedBy(PlayerRecord record) {
        return rule.test(record);
    }

    /** Tous les hauts faits, dans l'ordre de déclaration, avec leur état. */
    public static List<Earned> evaluate(PlayerRecord record) {
        return Arrays.stream(values())
                .map(achievement -> new Earned(achievement, achievement.isEarnedBy(record)))
                .toList();
    }

    public String displayName() {
        return displayName;
    }

    public String requirement() {
        return requirement;
    }

    /** Un haut fait et son état pour un joueur. */
    public record Earned(Achievement achievement, boolean earned) {
    }
}
