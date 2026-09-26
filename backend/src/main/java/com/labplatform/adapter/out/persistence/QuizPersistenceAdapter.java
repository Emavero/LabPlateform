package com.labplatform.adapter.out.persistence;

import com.labplatform.adapter.out.persistence.entity.QuizChoiceJpaEntity;
import com.labplatform.adapter.out.persistence.entity.QuizQuestionJpaEntity;
import com.labplatform.adapter.out.persistence.repository.SpringDataQuizChoiceRepository;
import com.labplatform.adapter.out.persistence.repository.SpringDataQuizQuestionRepository;
import com.labplatform.application.port.out.QuizRepositoryPort;
import com.labplatform.domain.academy.Quiz;
import com.labplatform.domain.academy.QuizChoice;
import com.labplatform.domain.academy.QuizQuestion;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Questions et propositions : deux tables, un seul objet. Elles sont
 * rechargées ensemble, en deux requêtes quel que soit le nombre de sections.
 */
@Component
public class QuizPersistenceAdapter implements QuizRepositoryPort {

    private final SpringDataQuizQuestionRepository questions;
    private final SpringDataQuizChoiceRepository choices;

    public QuizPersistenceAdapter(SpringDataQuizQuestionRepository questions,
                                  SpringDataQuizChoiceRepository choices) {
        this.questions = questions;
        this.choices = choices;
    }

    @Override
    public Quiz findBySection(Long sectionId) {
        return findBySections(List.of(sectionId)).getOrDefault(sectionId, Quiz.empty(sectionId));
    }

    @Override
    public Map<Long, Quiz> findBySections(Collection<Long> sectionIds) {
        if (sectionIds.isEmpty()) {
            return Map.of();
        }
        List<QuizQuestionJpaEntity> rows = questions.findBySectionIdInOrderByPosition(sectionIds);
        if (rows.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<QuizChoice>> choicesByQuestion =
                choices.findByQuestionIdInOrderByPosition(rows.stream().map(QuizQuestionJpaEntity::getId).toList())
                        .stream()
                        .collect(Collectors.groupingBy(QuizChoiceJpaEntity::getQuestionId,
                                Collectors.mapping(QuizPersistenceAdapter::toDomain, Collectors.toList())));

        Map<Long, List<QuizQuestion>> bySection = new HashMap<>();
        for (QuizQuestionJpaEntity row : rows) {
            List<QuizChoice> options = choicesByQuestion.getOrDefault(row.getId(), List.of());
            if (options.isEmpty()) {
                // Une question sans proposition ne se corrige pas : elle est ignorée.
                continue;
            }
            bySection.computeIfAbsent(row.getSectionId(), id -> new java.util.ArrayList<>())
                    .add(new QuizQuestion(row.getId(), row.getSectionId(), row.getStatement(), row.getPosition(),
                            options));
        }
        Map<Long, Quiz> quizzes = new HashMap<>();
        bySection.forEach((sectionId, list) -> quizzes.put(sectionId, new Quiz(sectionId, list)));
        return quizzes;
    }

    /** L'éditeur envoie toujours l'ensemble : les anciennes questions disparaissent. */
    @Override
    @Transactional
    public void replaceForSection(Long sectionId, List<QuizQuestion> wanted) {
        List<Long> previous = questions.findBySectionIdInOrderByPosition(List.of(sectionId)).stream()
                .map(QuizQuestionJpaEntity::getId)
                .toList();
        if (!previous.isEmpty()) {
            choices.deleteByQuestionIdIn(previous);
        }
        questions.deleteBySectionId(sectionId);

        int questionPosition = 1;
        for (QuizQuestion question : wanted) {
            QuizQuestionJpaEntity saved = questions.save(new QuizQuestionJpaEntity(null, sectionId,
                    question.statement(), questionPosition++));
            int choicePosition = 1;
            for (QuizChoice choice : question.choices()) {
                choices.save(new QuizChoiceJpaEntity(null, saved.getId(), choice.label(), choice.correct(),
                        choicePosition++));
            }
        }
    }

    private static QuizChoice toDomain(QuizChoiceJpaEntity e) {
        return new QuizChoice(e.getId(), e.getLabel(), e.isCorrect(), e.getPosition());
    }
}
