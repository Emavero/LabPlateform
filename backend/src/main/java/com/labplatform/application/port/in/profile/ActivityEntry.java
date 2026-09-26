package com.labplatform.application.port.in.profile;

import java.time.Instant;

/**
 * Ligne du journal d'activité d'un joueur.
 *
 * @param points points gagnés, 0 pour ce qui n'en rapporte pas
 * @param detail précision affichée sous le titre
 */
public record ActivityEntry(Kind kind, String title, String detail, int points, boolean firstBlood, Instant at) {

    public enum Kind {
        FLAG,
        SECTION
    }
}
