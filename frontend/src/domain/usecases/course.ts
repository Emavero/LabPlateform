import type { Course, CourseSummary, LearningProgress, Track } from '../models/Course';
import type { CourseRepository } from '../repositories/CourseRepository';

export class ListTracksUseCase {
  constructor(private readonly courses: CourseRepository) {}

  execute(): Promise<Track[]> {
    return this.courses.tracks();
  }
}

export class ListCoursesUseCase {
  constructor(private readonly courses: CourseRepository) {}

  execute(trackSlug?: string): Promise<CourseSummary[]> {
    return this.courses.list(trackSlug);
  }
}

export class GetCourseUseCase {
  constructor(private readonly courses: CourseRepository) {}

  execute(slug: string): Promise<Course> {
    return this.courses.get(slug);
  }
}

export class GetLearningProgressUseCase {
  constructor(private readonly courses: CourseRepository) {}

  execute(): Promise<LearningProgress[]> {
    return this.courses.progress();
  }
}

/** Coche ou décoche une section : un seul point d'entrée pour la case à cocher. */
export class ToggleSectionUseCase {
  constructor(private readonly courses: CourseRepository) {}

  execute(courseSlug: string, sectionSlug: string, completed: boolean): Promise<Course> {
    return completed
      ? this.courses.reopenSection(courseSlug, sectionSlug)
      : this.courses.completeSection(courseSlug, sectionSlug);
  }
}
