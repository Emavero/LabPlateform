import type { Difficulty } from './Box';
import type { CourseLevel, SectionKind, TopicCode } from './Course';
import type { OperatingSystem } from './VirtualMachine';

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
/** Question saisie dans l'éditeur, avec ses propositions. */
export interface ChoiceDraft {
  readonly label: string;
  readonly correct: boolean;
}

export interface QuestionDraft {
  readonly statement: string;
  readonly choices: readonly ChoiceDraft[];
}

export const EMPTY_QUESTION: QuestionDraft = {
  statement: '',
  choices: [
    { label: '', correct: true },
    { label: '', correct: false },
  ],
};

export interface SectionDraft {
  readonly id: number | null;
  readonly title: string;
  readonly kind: SectionKind;
  readonly minutes: number;
  readonly content: string;
  readonly videoUrl: string;
  readonly questions: readonly QuestionDraft[];
}

/** Étape de la chaîne d'attaque saisie. Sa position vient de l'ordre de la liste. */
export interface StageDraft {
  readonly name: string;
  readonly description: string;
  readonly technique: string;
}

/** Mise en situation : chez qui, quoi, quel enjeu, quelle issue. */
export interface CaseDraft {
  readonly sector: string;
  readonly situation: string;
  readonly stake: string;
  readonly outcome: string;
}

/** Concepteur du scénario. L'avatar est facultatif : à défaut, les initiales. */
export interface DesignerDraft {
  readonly name: string;
  readonly role: string;
  readonly avatarUrl: string;
}

/**
 * Cours en cours de saisie.
 * <p>
 * La filière ne s'y trouve pas : le sous-domaine la porte. L'éditeur fait
 * choisir la filière avant le sous-domaine, mais ce n'est là qu'une façon de
 * raccourcir la liste — seul le second est enregistré.
 */
export interface CourseDraft {
  readonly title: string;
  readonly topic: TopicCode;
  readonly level: CourseLevel;
  readonly summary: string;
  readonly sections: readonly SectionDraft[];
  /** Chemin d'attaque, cas d'usage, concepteurs : facultatifs d'un bout à l'autre. */
  readonly attackSummary: string;
  readonly stages: readonly StageDraft[];
  readonly realCase: CaseDraft;
  readonly designers: readonly DesignerDraft[];
}

export const EMPTY_STAGE: StageDraft = { name: '', description: '', technique: '' };

export const EMPTY_CASE: CaseDraft = { sector: '', situation: '', stake: '', outcome: '' };

export const EMPTY_DESIGNER: DesignerDraft = { name: '', role: '', avatarUrl: '' };

export const EMPTY_SECTION: SectionDraft = {
  id: null,
  title: '',
  kind: 'THEORY',
  minutes: 10,
  content: '',
  videoUrl: '',
  questions: [],
};

export const EMPTY_DRAFT: CourseDraft = {
  title: '',
  topic: 'EVIDENCE_HANDLING',
  level: 'FUNDAMENTAL',
  summary: '',
  sections: [EMPTY_SECTION],
  attackSummary: '',
  stages: [],
  realCase: EMPTY_CASE,
  designers: [],
};

/**
 * Les codes, dans l'ordre où l'éditeur les propose. Ce sont des codes et non
 * des libellés : le nom affiché vient du catalogue de traduction, pour que la
 * liste déroulante suive la langue choisie.
 */
export const COURSE_LEVELS: readonly CourseLevel[] = ['FUNDAMENTAL', 'EASY', 'MEDIUM', 'HARD'];

export const SECTION_KINDS: readonly SectionKind[] = ['THEORY', 'LAB', 'QUIZ'];


/** Machine du catalogue en cours de saisie. */
export interface BoxDraft {
  readonly name: string;
  readonly operatingSystem: OperatingSystem;
  readonly difficulty: Difficulty;
  readonly synopsis: string;
  readonly ipAddress: string;
  readonly maker: string;
  readonly retired: boolean;
  /** Réservée aux abonnés Pro. Vraie par défaut : une machine ne s'ouvre pas par oubli. */
  readonly proOnly: boolean;
  /** Laissé vide : tiré au hasard à la création, inchangé à la modification. */
  readonly userFlag: string;
  readonly rootFlag: string;
}

/**
 * Machine publiée. Les flags en clair n'arrivent qu'avec la réponse qui suit
 * leur tirage : ils ne sont plus jamais lisibles ensuite.
 */
export interface PublishedBox {
  readonly slug: string;
  readonly name: string;
  readonly os: OperatingSystem;
  readonly osName: string;
  readonly difficulty: Difficulty;
  readonly difficultyName: string;
  readonly userFlagPoints: number;
  readonly rootFlagPoints: number;
  readonly totalPoints: number;
  readonly synopsis: string;
  readonly ipAddress: string;
  readonly maker: string;
  readonly releasedAt: Date;
  readonly retired: boolean;
  readonly proOnly: boolean;
  readonly userFlagOnce: string | null;
  readonly rootFlagOnce: string | null;
}

export const EMPTY_BOX_DRAFT: BoxDraft = {
  name: '',
  operatingSystem: 'LINUX',
  difficulty: 'EASY',
  synopsis: '',
  ipAddress: '10.10.10.',
  maker: 'cyberMans',
  retired: false,
  proOnly: true,
  userFlag: '',
  rootFlag: '',
};

export const DIFFICULTIES: readonly Difficulty[] = ['VERY_EASY', 'EASY', 'MEDIUM', 'HARD', 'INSANE'];

export const OPERATING_SYSTEMS: readonly OperatingSystem[] = ['LINUX', 'WINDOWS'];

/** Fichier téléversé sur la plateforme. */
export interface UploadedMedia {
  readonly id: string;
  /** Adresse à référencer dans une section de cours. */
  readonly url: string;
  readonly filename: string;
  readonly contentType: string;
  readonly sizeBytes: number;
}

export const MEDIA_MAX_BYTES = 256 * 1024 * 1024;
export const MEDIA_TYPES = ['video/mp4', 'video/webm', 'video/ogg'] as const;

/** Refuse tout de suite ce que le serveur refuserait de toute façon. */
export function mediaError(file: { type: string; size: number }): string | undefined {
  if (!MEDIA_TYPES.includes(file.type as (typeof MEDIA_TYPES)[number])) {
    return 'Format non pris en charge. Attendu : MP4, WebM ou Ogg.';
  }
  if (file.size > MEDIA_MAX_BYTES) {
    return `Fichier trop lourd : ${Math.round(MEDIA_MAX_BYTES / (1024 * 1024))} Mo au plus.`;
  }
  return undefined;
}
