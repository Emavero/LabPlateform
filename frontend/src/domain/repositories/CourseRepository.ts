import type { Course, CourseSummary, LearningProgress, QuizAnswers, QuizResult, Track } from '../models/Course';

export interface CourseRepository {
  tracks(): Promise<Track[]>;
  list(trackSlug?: string): Promise<CourseSummary[]>;
  get(slug: string): Promise<Course>;
  progress(): Promise<LearningProgress[]>;
  completeSection(courseSlug: string, sectionSlug: string): Promise<Course>;
  reopenSection(courseSlug: string, sectionSlug: string): Promise<Course>;
  /** Rend la copie d'un quiz : la correction vient du serveur, jamais d'ici. */
  gradeQuiz(courseSlug: string, sectionSlug: string, answers: QuizAnswers): Promise<QuizResult>;
}
