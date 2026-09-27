package com.labplatform.domain.report;

/**
 * Ce qu'un compte a fait sur une période.
 *
 * @param flags             flags validés
 * @param boxesPwned        machines possédées de bout en bout (user et root)
 * @param points            points gagnés
 * @param sections          sections de cours terminées
 * @param quizzesPassed     quiz réussis
 * @param quizzesFailed     quiz manqués : un rapport honnête montre aussi les échecs
 * @param minutesStudied    durée annoncée des sections terminées
 * @param activeDays        jours distincts où le compte a agi
 * @param events            actes inscrits au journal
 */
public record ReportTotals(long flags, long boxesPwned, long points, long sections, long quizzesPassed,
                           long quizzesFailed, long minutesStudied, long activeDays, long events) {

    public static final ReportTotals EMPTY = new ReportTotals(0, 0, 0, 0, 0, 0, 0, 0, 0);

    /** Aucun acte sur la période : la page le dit plutôt que d'aligner des zéros. */
    public boolean isIdle() {
        return events == 0;
    }

    /**
     * Part des quiz réussis, ou -1 quand aucun quiz n'a été rendu.
     * <p>
     * Moins un, et non zéro : « aucun quiz rendu » et « tous manqués » ne se
     * lisent pas de la même façon, et l'affichage rend un tiret pour le premier.
     */
    public int quizSuccessPercent() {
        long graded = quizzesPassed + quizzesFailed;
        return graded == 0 ? -1 : (int) Math.round(100.0 * quizzesPassed / graded);
    }
}
