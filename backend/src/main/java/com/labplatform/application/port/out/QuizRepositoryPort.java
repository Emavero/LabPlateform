package com.labplatform.application.port.out;

import com.labplatform.domain.academy.Quiz;
import com.labplatform.domain.academy.QuizQuestion;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface QuizRepositoryPort {

    Quiz findBySection(Long sectionId);

    /** Quiz de plusieurs sections en une fois, pour afficher un cours entier. */
    Map<Long, Quiz> findBySections(Collection<Long> sectionIds);

    /** Remplace les questions d'une section : l'éditeur envoie toujours l'ensemble. */
    void replaceForSection(Long sectionId, List<QuizQuestion> questions);
}
