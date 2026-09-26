package com.labplatform.domain.academy;

import java.util.List;
import java.util.Set;

/**
 * Copie corrigée : ce qui est juste, ce qui ne l'est pas, et les bonnes
 * réponses — qui n'apparaissent qu'ici, après correction.
 */
public record QuizResult(List<Answer> answers, int questions) {

    public int correct() {
        return (int) answers.stream().filter(Answer::correct).count();
    }

    public double ratio() {
        return questions == 0 ? 0 : (double) correct() / questions;
    }

    /** Réussi : la section est alors marquée terminée. */
    public boolean isPassed() {
        return ratio() >= Quiz.PASS_RATIO;
    }

    /**
     * @param correctChoiceIds bonnes propositions, révélées avec la correction
     */
    public record Answer(Long questionId, boolean correct, Set<Long> correctChoiceIds) {
    }
}
