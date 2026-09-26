package com.labplatform.domain.academy;

import com.labplatform.domain.shared.InvalidInputException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Quiz d'une section : ses questions, et leur correction.
 * <p>
 * La correction se fait ici, jamais côté client : les bonnes réponses ne
 * sortent du serveur qu'avec le résultat, une fois la copie rendue.
 */
public record Quiz(Long sectionId, List<QuizQuestion> questions) {

    /** Part de bonnes réponses à atteindre pour valider la section. */
    public static final double PASS_RATIO = 0.7;

    public Quiz {
        questions = questions == null
                ? List.of()
                : questions.stream().sorted(Comparator.comparingInt(QuizQuestion::position)).toList();
    }

    public static Quiz empty(Long sectionId) {
        return new Quiz(sectionId, List.of());
    }

    public boolean isEmpty() {
        return questions.isEmpty();
    }

    /**
     * Corrige une copie.
     *
     * @param answers propositions cochées, par identifiant de question
     */
    public QuizResult grade(Map<Long, Set<Long>> answers) {
        if (isEmpty()) {
            throw new InvalidInputException("Cette section ne comporte pas de quiz");
        }
        Map<Long, Set<Long>> submitted = answers == null ? Map.of() : answers;
        List<QuizResult.Answer> outcomes = questions.stream()
                .map(question -> new QuizResult.Answer(question.id(),
                        question.isAnsweredBy(submitted.get(question.id())),
                        question.correctChoiceIds()))
                .toList();
        return new QuizResult(outcomes, questions.size());
    }
}
