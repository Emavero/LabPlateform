package com.labplatform.domain.exposure;

import java.util.List;

/**
 * Chemin de progression vers une cible : par où commencer, et dans quel ordre
 * continuer.
 * <p>
 * Ce n'est pas un chemin d'attaque découvert sur le réseau — la plateforme ne
 * scanne rien. C'est un ordre d'apprentissage déduit de ce qu'elle sait : les
 * segments déclarés, les systèmes, et la résistance mesurée de chaque cible.
 * Le dire est important : un joueur qui croirait lire une reconnaissance réelle
 * en tirerait de fausses conclusions.
 *
 * @param hops   étapes, de la plus abordable à l'objectif
 * @param effort somme des efforts : sert à comparer deux chemins vers la même cible
 */
public record AttackPath(String objectiveSlug, String objectiveName, List<AttackHop> hops, int effort) {

    /**
     * Étape du chemin.
     *
     * @param reason ce qui relie cette étape à la précédente (segment partagé,
     *               même système), vide pour la première
     */
    public record AttackHop(String slug, String name, ExposedService service, String segment, int effort,
                            AttackLink reason) {
    }

    /** Nature du lien entre deux étapes. */
    public enum AttackLink {

        ENTRY("Point d'entrée : la cible la moins résistante du lab"),
        SAME_SEGMENT("Même segment réseau : déplacement latéral plausible"),
        SAME_SYSTEM("Même système : les techniques et les identifiants se réemploient");

        private final String displayName;

        AttackLink(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }
}
