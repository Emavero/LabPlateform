import type { CaseDraft, CourseDraft, DesignerDraft, QuestionDraft, StageDraft } from '../models/Admin';

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
export const ATTACK_SUMMARY_MAX_LENGTH = 1024;

export interface DraftErrors {
  readonly title?: string;
  readonly summary?: string;
  readonly sections?: string;
  /** Erreurs par section, indexées par position dans la liste. */
  readonly bySection: Readonly<Record<number, string>>;
  /** Chemin d'attaque, cas d'usage réel, concepteurs : un message chacun. */
  readonly attackPath?: string;
  readonly realCase?: string;
  readonly designers?: string;
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
    attackPath: attackPathError(draft),
    realCase: realCaseError(draft.realCase),
    designers: designersError(draft.designers),
  };
}

/** Lignes laissées entièrement vides : l'auteur n'a rien saisi, rien à signaler. */
function isEmptyStage(stage: StageDraft): boolean {
  return !stage.name.trim() && !stage.description.trim() && !stage.technique.trim();
}

function isEmptyDesigner(designer: DesignerDraft): boolean {
  return !designer.name.trim() && !designer.role.trim() && !designer.avatarUrl.trim();
}

/**
 * Un chemin d'attaque tient ou tombe entier : un résumé sans étapes n'illustre
 * aucune chaîne, et des étapes sans résumé ne disent pas de quoi elles sont la
 * chaîne.
 */
function attackPathError(draft: CourseDraft): string | undefined {
  const summary = draft.attackSummary.trim();
  const filled = draft.stages.filter((stage) => !isEmptyStage(stage));

  if (summary.length > ATTACK_SUMMARY_MAX_LENGTH) {
    return `Le résumé du chemin d'attaque est limité à ${ATTACK_SUMMARY_MAX_LENGTH} caractères.`;
  }
  if (summary && filled.length === 0) return 'Ajoutez au moins une étape, ou retirez le résumé.';
  if (!summary && filled.length > 0) return 'Décrivez en une phrase ce que ces étapes enchaînent.';

  for (const [index, stage] of filled.entries()) {
    const numbered = `Étape ${index + 1} : `;
    if (!stage.name.trim()) return `${numbered}le nom est obligatoire.`;
    if (!stage.description.trim()) return `${numbered}la description est obligatoire.`;
  }
  return undefined;
}

/** Un cas d'usage se décrit entièrement ou pas du tout. */
function realCaseError(realCase: CaseDraft): string | undefined {
  const values = [realCase.sector, realCase.situation, realCase.stake, realCase.outcome].map((v) => v.trim());
  const filled = values.filter(Boolean).length;
  if (filled === 0 || filled === 4) return undefined;
  return 'Remplissez les quatre champs du cas d’usage, ou laissez-les tous vides.';
}

function designersError(designers: readonly DesignerDraft[]): string | undefined {
  for (const [index, designer] of designers.filter((d) => !isEmptyDesigner(d)).entries()) {
    const numbered = `Concepteur ${index + 1} : `;
    if (!designer.name.trim()) return `${numbered}le nom est obligatoire.`;
    if (!designer.role.trim()) return `${numbered}le rôle est obligatoire.`;
  }
  return undefined;
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
  return (
    Boolean(errors.title || errors.summary || errors.sections) ||
    Boolean(errors.attackPath || errors.realCase || errors.designers) ||
    Object.keys(errors.bySection).length > 0
  );
}
