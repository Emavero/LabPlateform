package com.labplatform.domain.academy;

import com.labplatform.domain.shared.InvalidInputException;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Question d'un quiz, avec ses propositions.
 * <p>
 * Une question peut attendre plusieurs bonnes réponses : elle n'est acquise
 * que si l'apprenant coche exactement celles-là, ni plus, ni moins. Cocher
 * tout ne rapporte donc rien.
 */
public record QuizQuestion(Long id, Long sectionId, String statement, int position, List<QuizChoice> choices) {

    private static final int MIN_CHOICES = 2;
    private static final int MAX_CHOICES = 6;
    private static final int MAX_STATEMENT_LENGTH = 512;

    public QuizQuestion {
        Objects.requireNonNull(choices, "choices");
        if (statement == null || statement.isBlank()) {
            throw new InvalidInputException("L'énoncé de la question est obligatoire");
        }
        statement = statement.trim();
        if (statement.length() > MAX_STATEMENT_LENGTH) {
            throw new InvalidInputException("L'énoncé est limité à " + MAX_STATEMENT_LENGTH + " caractères");
        }
        if (position < 1) {
            throw new IllegalArgumentException("La première question porte le numéro 1");
        }
        choices = choices.stream().sorted(Comparator.comparingInt(QuizChoice::position)).toList();
        if (choices.size() < MIN_CHOICES || choices.size() > MAX_CHOICES) {
            throw new InvalidInputException("Une question compte de " + MIN_CHOICES + " à " + MAX_CHOICES
                    + " propositions");
        }
        if (choices.stream().noneMatch(QuizChoice::correct)) {
            throw new InvalidInputException("Une question a au moins une bonne réponse");
        }
    }

    /** Identifiants des propositions correctes. */
    public Set<Long> correctChoiceIds() {
        return choices.stream().filter(QuizChoice::correct).map(QuizChoice::id).collect(Collectors.toSet());
    }

    /** Vrai si l'apprenant a coché exactement les bonnes propositions. */
    public boolean isAnsweredBy(Set<Long> chosen) {
        return correctChoiceIds().equals(chosen == null ? Set.of() : chosen);
    }
}
