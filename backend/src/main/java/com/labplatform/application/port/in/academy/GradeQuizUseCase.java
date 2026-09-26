package com.labplatform.application.port.in.academy;

import com.labplatform.domain.academy.QuizResult;
import com.labplatform.domain.user.Actor;

import java.util.Map;
import java.util.Set;

public interface GradeQuizUseCase {

    /**
     * Corrige la copie de l'appelant. Réussie, elle marque la section comme
     * terminée : inutile de la cocher en plus.
     *
     * @throws com.labplatform.domain.shared.NotFoundException     cours ou section inconnus
     * @throws com.labplatform.domain.shared.InvalidInputException la section ne comporte pas de quiz
     */
    QuizResult grade(Actor actor, String courseSlug, String sectionSlug, Map<Long, Set<Long>> answers);
}
