import type { CourseDraft } from '../models/Admin';

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
    } else if (section.videoUrl.trim() && !/^https?:\/\//i.test(section.videoUrl.trim())) {
      bySection[index] = "L'adresse de la vidéo doit commencer par https://.";
    } else if (!section.content.trim() && !section.videoUrl.trim()) {
      bySection[index] = 'Une section porte au moins un texte ou une vidéo.';
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

export function draftHasErrors(errors: DraftErrors): boolean {
  return Boolean(errors.title || errors.summary || errors.sections) || Object.keys(errors.bySection).length > 0;
}
