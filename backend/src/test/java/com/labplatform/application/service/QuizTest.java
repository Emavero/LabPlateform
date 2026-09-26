package com.labplatform.application.service;

import com.labplatform.application.fakes.Fakes;
import com.labplatform.application.fakes.InMemoryCompletions;
import com.labplatform.application.fakes.InMemoryCourses;
import com.labplatform.application.fakes.InMemoryQuizzes;
import com.labplatform.domain.academy.Course;
import com.labplatform.domain.academy.CourseLevel;
import com.labplatform.domain.academy.CourseSection;
import com.labplatform.domain.academy.Quiz;
import com.labplatform.domain.academy.QuizChoice;
import com.labplatform.domain.academy.QuizQuestion;
import com.labplatform.domain.academy.QuizResult;
import com.labplatform.domain.academy.SectionKind;
import com.labplatform.domain.academy.Track;
import com.labplatform.domain.shared.InvalidInputException;
import com.labplatform.domain.user.Actor;
import com.labplatform.domain.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Correction d'un quiz : le barème, et ce que la réussite déclenche. */
class QuizTest {

    private static final Actor ALICE = new Actor(1L, Role.USER);
    private static final Instant NOW = Instant.parse("2026-09-26T09:00:00Z");

    private InMemoryQuizzes quizzes;
    private AcademyService academy;
    private Long sectionId;

    @BeforeEach
    void setUp() {
        InMemoryCourses courses = new InMemoryCourses();
        InMemoryCompletions completions = new InMemoryCompletions();
        quizzes = new InMemoryQuizzes();
        academy = new AcademyService(courses, completions, quizzes, Fakes.NO_TRANSACTION,
                Clock.fixed(NOW, ZoneOffset.UTC));

        Course saved = courses.save(Course.create("traces", "Traces", Track.FORENSICS, CourseLevel.FUNDAMENTAL,
                "Résumé.", NOW, List.of(
                        CourseSection.of(null, "lecon", "Leçon", SectionKind.THEORY, 1, 10, "Contenu"),
                        CourseSection.of(null, "quiz", "Quiz", SectionKind.QUIZ, 2, 10, "Répondez."))));
        sectionId = saved.getSections().get(1).id();
        quizzes.replaceForSection(sectionId, List.of(
                question("Ordre de volatilité ?", 1, "Mémoire vive", true, "Sauvegardes", false),
                question("Que faire en premier ?", 2, "Capturer la mémoire", true, "Éteindre", false),
                question("Quelles traces sont volatiles ?", 3, "Connexions", true, "Disque", false)));
    }

    private static QuizQuestion question(String statement, int position, String first, boolean firstCorrect,
                                         String second, boolean secondCorrect) {
        return new QuizQuestion(null, null, statement, position,
                List.of(new QuizChoice(null, first, firstCorrect, 1), new QuizChoice(null, second, secondCorrect, 2)));
    }

    private Map<Long, Set<Long>> answers(boolean... rights) {
        List<QuizQuestion> questions = quizzes.findBySection(sectionId).questions();
        Map<Long, Set<Long>> submitted = new java.util.HashMap<>();
        for (int i = 0; i < rights.length; i++) {
            QuizQuestion question = questions.get(i);
            QuizChoice choice = question.choices().get(rights[i] ? 0 : 1);
            submitted.put(question.id(), Set.of(choice.id()));
        }
        return submitted;
    }

    @Test
    void aPerfectCopyPassesAndCompletesTheSection() {
        QuizResult result = academy.grade(ALICE, "traces", "quiz", answers(true, true, true));

        assertEquals(3, result.correct());
        assertEquals(1.0, result.ratio(), 1e-9);
        assertTrue(result.isPassed());
        // La section est validée par le quiz : inutile de la cocher en plus.
        assertTrue(academy.getCourse(ALICE, "traces").completedSectionIds().contains(sectionId));
    }

    @Test
    void aCopyBelowTheThresholdFailsAndCompletesNothing() {
        QuizResult result = academy.grade(ALICE, "traces", "quiz", answers(true, false, false));

        assertEquals(1, result.correct());
        assertFalse(result.isPassed());
        assertFalse(academy.getCourse(ALICE, "traces").completedSectionIds().contains(sectionId));
    }

    @Test
    void theThresholdIsSeventyPercent() {
        // Deux bonnes réponses sur trois : 66 %, sous le seuil.
        assertFalse(academy.grade(ALICE, "traces", "quiz", answers(true, true, false)).isPassed());
        assertEquals(0.7, Quiz.PASS_RATIO, 1e-9);
    }

    @Test
    void theCorrectionRevealsTheExpectedAnswers() {
        QuizResult result = academy.grade(ALICE, "traces", "quiz", answers(false, false, false));

        assertEquals(3, result.answers().size());
        assertTrue(result.answers().stream().noneMatch(QuizResult.Answer::correct));
        // Les bonnes réponses n'apparaissent qu'ici, une fois la copie rendue.
        assertTrue(result.answers().stream().allMatch(answer -> answer.correctChoiceIds().size() == 1));
    }

    @Test
    void anUnansweredQuestionIsSimplyWrong() {
        QuizResult result = academy.grade(ALICE, "traces", "quiz", Map.of());

        assertEquals(0, result.correct());
        assertFalse(result.isPassed());
    }

    @Test
    void aQuestionWithSeveralCorrectChoicesDemandsExactlyThose() {
        QuizQuestion question = new QuizQuestion(1L, sectionId, "Lesquelles sont volatiles ?", 1, List.of(
                new QuizChoice(10L, "Mémoire", true, 1),
                new QuizChoice(11L, "Connexions", true, 2),
                new QuizChoice(12L, "Sauvegardes", false, 3)));

        assertTrue(question.isAnsweredBy(Set.of(10L, 11L)));
        // Une bonne réponse sur deux ne suffit pas, et cocher tout non plus.
        assertFalse(question.isAnsweredBy(Set.of(10L)));
        assertFalse(question.isAnsweredBy(Set.of(10L, 11L, 12L)));
        assertFalse(question.isAnsweredBy(Set.of()));
    }

    @Test
    void aSectionWithoutQuizCannotBeGraded() {
        assertThrows(InvalidInputException.class, () -> academy.grade(ALICE, "traces", "lecon", Map.of()));
    }

    @Test
    void aQuestionWithoutCorrectChoiceIsRefused() {
        assertThrows(InvalidInputException.class, () -> new QuizQuestion(null, sectionId, "Sans réponse", 1,
                List.of(new QuizChoice(null, "A", false, 1), new QuizChoice(null, "B", false, 2))));
        assertThrows(InvalidInputException.class, () -> new QuizQuestion(null, sectionId, "Une seule", 1,
                List.of(new QuizChoice(null, "A", true, 1))));
    }
}
