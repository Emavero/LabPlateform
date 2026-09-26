package com.labplatform.application.fakes;

import com.labplatform.application.port.out.QuizRepositoryPort;
import com.labplatform.domain.academy.Quiz;
import com.labplatform.domain.academy.QuizChoice;
import com.labplatform.domain.academy.QuizQuestion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InMemoryQuizzes implements QuizRepositoryPort {

    private final Map<Long, List<QuizQuestion>> store = new LinkedHashMap<>();
    private long sequence = 0;

    @Override
    public Quiz findBySection(Long sectionId) {
        return new Quiz(sectionId, store.getOrDefault(sectionId, List.of()));
    }

    @Override
    public Map<Long, Quiz> findBySections(Collection<Long> sectionIds) {
        Map<Long, Quiz> quizzes = new HashMap<>();
        sectionIds.stream()
                .filter(store::containsKey)
                .forEach(sectionId -> quizzes.put(sectionId, findBySection(sectionId)));
        return quizzes;
    }

    /** Attribue un identifiant aux questions et aux propositions, comme la base. */
    @Override
    public void replaceForSection(Long sectionId, List<QuizQuestion> wanted) {
        if (wanted.isEmpty()) {
            store.remove(sectionId);
            return;
        }
        List<QuizQuestion> stored = new ArrayList<>();
        for (QuizQuestion question : wanted) {
            List<QuizChoice> choices = new ArrayList<>();
            for (QuizChoice choice : question.choices()) {
                choices.add(new QuizChoice(++sequence, choice.label(), choice.correct(), choice.position()));
            }
            stored.add(new QuizQuestion(++sequence, sectionId, question.statement(), question.position(), choices));
        }
        store.put(sectionId, stored);
    }
}
