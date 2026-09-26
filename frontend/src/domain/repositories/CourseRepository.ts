import type { Course, CourseSummary, LearningProgress, Track } from '../models/Course';

export interface CourseRepository {
  tracks(): Promise<Track[]>;
  list(trackSlug?: string): Promise<CourseSummary[]>;
  get(slug: string): Promise<Course>;
  progress(): Promise<LearningProgress[]>;
  completeSection(courseSlug: string, sectionSlug: string): Promise<Course>;
  reopenSection(courseSlug: string, sectionSlug: string): Promise<Course>;
}
