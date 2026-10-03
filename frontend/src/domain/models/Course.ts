export type TrackCode = 'FORENSICS' | 'DEFENSE';

/**
 * Sous-domaine d'une filière. Miroir de domain/academy/CourseTopic côté
 * serveur : c'est lui qui dit à quelle filière chacun appartient, la liste des
 * boutons de filtre vient donc de l'API et non d'ici.
 */
export type TopicCode =
  | 'EVIDENCE_HANDLING'
  | 'MEMORY_ANALYSIS'
  | 'DISK_FORENSICS'
  | 'LOG_ANALYSIS'
  | 'NETWORK_FORENSICS'
  | 'TIMELINE'
  | 'MALWARE_ANALYSIS'
  | 'HARDENING'
  | 'SIEM_SOC'
  | 'FIREWALLS'
  | 'INCIDENT_RESPONSE'
  | 'THREAT_HUNTING'
  | 'IDENTITY_ACCESS';
export type CourseLevel = 'FUNDAMENTAL' | 'EASY' | 'MEDIUM' | 'HARD';
export type SectionKind = 'THEORY' | 'LAB' | 'QUIZ';

/** Filière de cours, telle que le serveur la décrit : le menu en dépend. */
export interface Track {
  readonly track: TrackCode;
  readonly slug: string;
  readonly name: string;
  readonly description: string;
  /** Sous-domaines de la filière : les filtres de la page des cours. */
  readonly topics: readonly Topic[];
}

/** Sous-domaine : un bouton de filtre, sous sa filière. */
export interface Topic {
  readonly topic: TopicCode;
  readonly slug: string;
  readonly name: string;
  readonly track: TrackCode;
}

/** Cours dans une liste : pas de contenu, juste de quoi choisir. */
export interface CourseSummary {
  readonly slug: string;
  readonly title: string;
  readonly track: TrackCode;
  readonly trackName: string;
  readonly trackSlug: string;
  readonly topic: TopicCode;
  readonly topicName: string;
  readonly topicSlug: string;
  readonly level: CourseLevel;
  readonly levelName: string;
  readonly summary: string;
  readonly sections: number;
  readonly sectionsCompleted: number;
  readonly minutes: number;
  readonly completed: boolean;
  readonly started: boolean;
  readonly publishedAt: Date;
}

/** Proposition d'une question. `correct` n'arrive qu'après correction. */
export interface QuizChoice {
  readonly id: number;
  readonly label: string;
  readonly correct: boolean | null;
}

export interface QuizQuestion {
  readonly id: number;
  readonly statement: string;
  readonly position: number;
  readonly choices: readonly QuizChoice[];
}

/** Copie corrigée par le serveur. */
export interface QuizResult {
  readonly correct: number;
  readonly questions: number;
  readonly ratio: number;
  readonly passed: boolean;
  readonly answers: readonly {
    readonly questionId: number;
    readonly correct: boolean;
    readonly correctChoiceIds: readonly number[];
  }[];
}

/** Réponses cochées, par question. */
export type QuizAnswers = Readonly<Record<number, readonly number[]>>;

export interface CourseSection {
  /** Identifiant renvoyé tel quel par l'éditeur d'administration. */
  readonly id: number;
  readonly slug: string;
  readonly title: string;
  readonly kind: SectionKind;
  readonly kindName: string;
  readonly position: number;
  readonly minutes: number;
  readonly content: string;
  readonly videoUrl: string | null;
  readonly completed: boolean;
  readonly questions: readonly QuizQuestion[];
}

export function hasQuiz(section: CourseSection): boolean {
  return section.questions.length > 0;
}

/** Toutes les questions ont-elles reçu au moins une réponse ? */
export function isQuizComplete(section: CourseSection, answers: QuizAnswers): boolean {
  return section.questions.every((question) => (answers[question.id]?.length ?? 0) > 0);
}

/** Étape d'une chaîne d'attaque. La technique n'est pas toujours référencée. */
export interface AttackStage {
  readonly position: number;
  readonly name: string;
  readonly description: string;
  readonly technique: string | null;
}

/** Chemin d'attaque du cours : le vecteur, puis la suite d'étapes. */
export interface AttackPath {
  readonly summary: string;
  readonly stages: readonly AttackStage[];
}

/** Mise en situation : chez qui, quoi, quel enjeu, quelle issue. */
export interface RealCase {
  readonly sector: string;
  readonly situation: string;
  readonly stake: string;
  readonly outcome: string;
}

/** Concepteur du scénario. Sans avatar, la fiche montre ses initiales. */
export interface CourseDesigner {
  readonly name: string;
  readonly role: string;
  readonly avatarUrl: string | null;
  readonly initials: string;
}

export interface Course {
  readonly slug: string;
  readonly title: string;
  readonly track: TrackCode;
  readonly trackName: string;
  readonly trackSlug: string;
  readonly topic: TopicCode;
  readonly topicName: string;
  readonly topicSlug: string;
  readonly level: CourseLevel;
  readonly levelName: string;
  readonly summary: string;
  readonly minutes: number;
  readonly sectionsCompleted: number;
  readonly completed: boolean;
  readonly publishedAt: Date;
  readonly sections: readonly CourseSection[];
  /** Absents tant que l'auteur ne les a pas écrits : la page s'en passe. */
  readonly attackPath: AttackPath | null;
  readonly realCase: RealCase | null;
  readonly designers: readonly CourseDesigner[];
}

/** Avancement sur une filière entière. */
export interface LearningProgress {
  readonly track: TrackCode;
  readonly trackName: string;
  readonly trackSlug: string;
  readonly courses: number;
  readonly coursesCompleted: number;
  readonly sections: number;
  readonly sectionsCompleted: number;
  readonly minutesDone: number;
  readonly ratio: number;
}

/** Part du cours terminée, entre 0 et 1. */
export function courseRatio(course: Pick<CourseSummary, 'sections' | 'sectionsCompleted'>): number {
  return course.sections === 0 ? 0 : course.sectionsCompleted / course.sections;
}

/** Durée lisible : « 1 h 20 » plutôt que « 80 min ». */
export function formatDuration(minutes: number): string {
  if (minutes < 60) return `${minutes} min`;
  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  return rest === 0 ? `${hours} h` : `${hours} h ${String(rest).padStart(2, '0')}`;
}

/**
 * Première section d'atelier, s'il y en a une.
 * <p>
 * Un atelier se fait sur la machine cible : c'est là, et pas ailleurs, qu'il
 * faut pouvoir l'allumer. Le besoin se déduit du contenu du cours plutôt que
 * d'une case à cocher de plus dans l'éditeur — un cours qui porte un atelier
 * porte par là même ses commandes à exécuter.
 */
export function firstLabSection(course: Pick<Course, 'sections'>): CourseSection | undefined {
  return course.sections.find((section) => section.kind === 'LAB');
}

/** Première section non terminée : là où l'on reprend sa lecture. */
export function nextSection(course: Course): CourseSection | undefined {
  return course.sections.find((section) => !section.completed);
}
