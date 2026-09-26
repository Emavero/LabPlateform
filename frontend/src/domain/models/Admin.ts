import type { CourseLevel, SectionKind, TrackCode } from './Course';

/** Chiffres du tableau de bord d'administration. */
export interface AdminOverview {
  readonly users: number;
  readonly boxes: number;
  readonly courses: number;
  readonly sections: number;
  readonly flagsValidated: number;
  readonly sectionsCompleted: number;
}

/**
 * Section en cours de saisie. L'identifiant, repris de la lecture du cours,
 * est ce qui permet de renommer une section sans effacer l'avancement des
 * apprenants ; une nouvelle section n'en a pas.
 */
export interface SectionDraft {
  readonly id: number | null;
  readonly title: string;
  readonly kind: SectionKind;
  readonly minutes: number;
  readonly content: string;
  readonly videoUrl: string;
}

export interface CourseDraft {
  readonly title: string;
  readonly track: TrackCode;
  readonly level: CourseLevel;
  readonly summary: string;
  readonly sections: readonly SectionDraft[];
}

export const EMPTY_SECTION: SectionDraft = {
  id: null,
  title: '',
  kind: 'THEORY',
  minutes: 10,
  content: '',
  videoUrl: '',
};

export const EMPTY_DRAFT: CourseDraft = {
  title: '',
  track: 'FORENSICS',
  level: 'FUNDAMENTAL',
  summary: '',
  sections: [EMPTY_SECTION],
};

export const LEVEL_LABELS: Record<CourseLevel, string> = {
  FUNDAMENTAL: 'Fondamental',
  EASY: 'Facile',
  MEDIUM: 'Intermédiaire',
  HARD: 'Avancé',
};

export const KIND_LABELS: Record<SectionKind, string> = {
  THEORY: 'Cours',
  LAB: 'Atelier',
  QUIZ: 'Quiz',
};
