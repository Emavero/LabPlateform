package com.labplatform.domain.academy;

import com.labplatform.domain.shared.InvalidInputException;

/**
 * Cas d'usage réel : la même matière, vue depuis un métier.
 * <p>
 * Les quatre champs répondent à quatre questions, dans l'ordre où on se les
 * pose : chez qui, que s'est-il passé, qu'est-ce qui était en jeu, comment cela
 * s'est-il terminé. Ils tiennent ou tombent ensemble — un cas sans enjeu n'est
 * qu'une anecdote, et un enjeu sans situation n'est qu'une affirmation.
 */
public record RealWorldCase(String sector, String situation, String stake, String outcome) {

    private static final RealWorldCase NONE = new RealWorldCase(null, null, null, null);

    public RealWorldCase {
        sector = trim(sector);
        situation = trim(situation);
        stake = trim(stake);
        outcome = trim(outcome);
        int filled = count(sector) + count(situation) + count(stake) + count(outcome);
        if (filled != 0 && filled != 4) {
            throw new InvalidInputException("Un cas d'usage réel se décrit entièrement ou pas du tout");
        }
    }

    public static RealWorldCase none() {
        return NONE;
    }

    public static RealWorldCase of(String sector, String situation, String stake, String outcome) {
        return new RealWorldCase(sector, situation, stake, outcome);
    }

    public boolean isPresent() {
        return sector != null;
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static int count(String value) {
        return value == null ? 0 : 1;
    }
}
