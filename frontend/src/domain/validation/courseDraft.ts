import type { CourseDraft, QuestionDraft } from '../models/Admin';

/** Vidéo hébergée par la plateforme : l'adresse que renvoie le téléversement. */
const HOSTED_VIDEO = /^\/api\/media\/[0-9a-f]{32}$/;

/**
 * Règles de saisie de l'éditeur de cours. Miroir de celles du serveur, qui
 * reste l'autorité : ici, elles évitent un aller-retour pour une erreur
 * évidente.
 */
export const TITLE_MAX_LENGTH = 128;
export const SUMMARY_MAX_LENGTH = 512;
export const SECTION_MAX_MINUTES = 600;

export interface DraftErrors {
  readonly title?: string;
  readonly summary?: string;
  readonly sections?: string;
  /** Erreurs par section, indexées par position dans la liste. */
  readonly bySection: Readonly<Record<number, string>>;
}

export function validateDraft(draft: CourseDraft): DraftErrors {
  const bySection: Record<number, string> = {};
  draft.sections.forEach((section, index) => {
    if (!section.title.trim()) {
      bySection[index] = 'Le titre de la section est obligatoire.';
    } else if (section.title.length > TITLE_MAX_LENGTH) {
      bySection[index] = `Le titre est limité à ${TITLE_MAX_LENGTH} caractères.`;
    } else if (section.minutes < 0 || section.minutes > SECTION_MAX_MINUTES) {
      bySection[index] = `La durée va de 0 à ${SECTION_MAX_MINUTES} minutes.`;
    } else if (section.videoUrl.trim() && !isVideoUrl(section.videoUrl.trim())) {
      bySection[index] = 'La vidéo doit être une adresse https:// ou un fichier téléversé.';
    } else if (!section.content.trim() && !section.videoUrl.trim() && section.questions.length === 0) {
      bySection[index] = 'Une section porte au moins un texte, une vidéo ou un quiz.';
    } else {
      const quiz = quizError(section.questions);
      if (quiz) bySection[index] = quiz;
    }
  });

  return {
    title: !draft.title.trim()
      ? 'Le titre est obligatoire.'
      : draft.title.length > TITLE_MAX_LENGTH
        ? `Le titre est limité à ${TITLE_MAX_LENGTH} caractères.`
        : undefined,
    summary: draft.summary.length > SUMMARY_MAX_LENGTH
      ? `Le résumé est limité à ${SUMMARY_MAX_LENGTH} caractères.`
      : undefined,
    sections: draft.sections.length === 0 ? 'Un cours comporte au moins une section.' : undefined,
    bySection,
  };
}

function isVideoUrl(url: string): boolean {
  return HOSTED_VIDEO.test(url) || /^https?:\/\//i.test(url);
}

/** Une question se corrige seule : il lui faut un énoncé et une bonne réponse. */
function quizError(questions: readonly QuestionDraft[]): string | undefined {
  for (const [index, question] of questions.entries()) {
    const numbered = `Question ${index + 1} : `;
    if (!question.statement.trim()) return `${numbered}l'énoncé est obligatoire.`;
    if (question.choices.length < 2) return `${numbered}au moins deux propositions.`;
    if (question.choices.some((choice) => !choice.label.trim())) {
      return `${numbered}une proposition ne peut pas être vide.`;
    }
    if (!question.choices.some((choice) => choice.correct)) {
      return `${numbered}cochez au moins une bonne réponse.`;
    }
  }
  return undefined;
}

export function draftHasErrors(errors: DraftErrors): boolean {
  return Boolean(errors.title || errors.summary || errors.sections) || Object.keys(errors.bySection).length > 0;
}
