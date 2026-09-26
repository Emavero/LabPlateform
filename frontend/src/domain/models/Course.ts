export type TrackCode = 'FORENSICS' | 'DEFENSE';
export type CourseLevel = 'FUNDAMENTAL' | 'EASY' | 'MEDIUM' | 'HARD';
export type SectionKind = 'THEORY' | 'LAB' | 'QUIZ';

/** Filière de cours, telle que le serveur la décrit : le menu en dépend. */
export interface Track {
  readonly track: TrackCode;
  readonly slug: string;
  readonly name: string;
  readonly description: string;
}

/** Cours dans une liste : pas de contenu, juste de quoi choisir. */
export interface CourseSummary {
  readonly slug: string;
  readonly title: string;
  readonly track: TrackCode;
  readonly trackName: string;
  readonly trackSlug: string;
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

export interface CourseSection {
  readonly slug: string;
  readonly title: string;
  readonly kind: SectionKind;
  readonly kindName: string;
  readonly position: number;
  readonly minutes: number;
  readonly content: string;
  readonly completed: boolean;
}

export interface Course {
  readonly slug: string;
  readonly title: string;
  readonly track: TrackCode;
  readonly trackName: string;
  readonly trackSlug: string;
  readonly level: CourseLevel;
  readonly levelName: string;
  readonly summary: string;
  readonly minutes: number;
  readonly sectionsCompleted: number;
  readonly completed: boolean;
  readonly publishedAt: Date;
  readonly sections: readonly CourseSection[];
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

/** Première section non terminée : là où l'on reprend sa lecture. */
export function nextSection(course: Course): CourseSection | undefined {
  return course.sections.find((section) => !section.completed);
}
