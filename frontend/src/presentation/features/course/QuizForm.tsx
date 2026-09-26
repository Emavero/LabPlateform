import { useState, type FormEvent } from 'react';
import { isQuizComplete, type CourseSection, type QuizAnswers, type QuizResult } from '@/domain/models/Course';
import { Alert, Button, Icon } from '../../design-system';

interface QuizFormProps {
  section: CourseSection;
  result: QuizResult | undefined;
  grading: boolean;
  onSubmit: (answers: QuizAnswers) => void;
}

/**
 * Quiz corrigé par le serveur. Plusieurs bonnes réponses sont possibles : une
 * question n'est acquise que si l'apprenant coche exactement celles-là.
 */
export function QuizForm({ section, result, grading, onSubmit }: QuizFormProps) {
  const [answers, setAnswers] = useState<QuizAnswers>({});

  const toggle = (questionId: number, choiceId: number) =>
    setAnswers((current) => {
      const chosen = current[questionId] ?? [];
      return {
        ...current,
        [questionId]: chosen.includes(choiceId)
          ? chosen.filter((id) => id !== choiceId)
          : [...chosen, choiceId],
      };
    });

  const outcomeOf = (questionId: number) => result?.answers.find((answer) => answer.questionId === questionId);

  function submit(event: FormEvent) {
    event.preventDefault();
    onSubmit(answers);
  }

  return (
    <form className="quiz" onSubmit={submit} noValidate>
      {result && (
        <Alert
          tone={result.passed ? 'success' : 'error'}
          title={`${result.correct} bonne${result.correct > 1 ? 's' : ''} réponse${
            result.correct > 1 ? 's' : ''
          } sur ${result.questions}`}
        >
          {result.passed
            ? 'Quiz réussi : la section est validée.'
            : 'Il faut 70 % de bonnes réponses. Les bonnes réponses sont indiquées ci-dessous.'}
        </Alert>
      )}

      {section.questions.map((question) => {
        const outcome = outcomeOf(question.id);
        return (
          <fieldset
            key={question.id}
            className={['quiz__question', outcome && (outcome.correct ? 'quiz__question--right' : 'quiz__question--wrong')]
              .filter(Boolean)
              .join(' ')}
          >
            <legend className="quiz__statement">
              {question.position}. {question.statement}
              {outcome && <Icon name={outcome.correct ? 'check' : 'stop'} size={15} />}
            </legend>
            {question.choices.map((choice) => {
              const expected = outcome?.correctChoiceIds.includes(choice.id) ?? false;
              return (
                <label
                  key={choice.id}
                  className={['quiz__choice', outcome && expected && 'quiz__choice--expected']
                    .filter(Boolean)
                    .join(' ')}
                >
                  <input
                    type="checkbox"
                    checked={(answers[question.id] ?? []).includes(choice.id)}
                    onChange={() => toggle(question.id, choice.id)}
                    disabled={grading}
                  />
                  <span>{choice.label}</span>
                  {outcome && expected && <Icon name="check" size={13} />}
                </label>
              );
            })}
          </fieldset>
        );
      })}

      <Button
        type="submit"
        icon="check"
        loading={grading}
        loadingLabel="Correction…"
        disabled={!isQuizComplete(section, answers)}
      >
        {result ? 'Corriger à nouveau' : 'Rendre ma copie'}
      </Button>
    </form>
  );
}
