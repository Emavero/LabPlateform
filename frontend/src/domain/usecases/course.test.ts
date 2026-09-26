import { describe, expect, it } from 'vitest';
import type { Course, CourseSummary, LearningProgress, Track } from '../models/Course';
import type { CourseRepository } from '../repositories/CourseRepository';
import { ToggleSectionUseCase } from './course';

function repository(calls: string[]): CourseRepository {
  return {
    tracks: async () => [] as Track[],
    list: async () => [] as CourseSummary[],
    get: async () => ({}) as Course,
    progress: async () => [] as LearningProgress[],
    completeSection: async (courseSlug, sectionSlug) => {
      calls.push(`complete:${courseSlug}/${sectionSlug}`);
      return {} as Course;
    },
    reopenSection: async (courseSlug, sectionSlug) => {
      calls.push(`reopen:${courseSlug}/${sectionSlug}`);
      return {} as Course;
    },
  };
}

describe('ToggleSectionUseCase', () => {
  it("coche une section non terminée et décoche l'inverse", async () => {
    const calls: string[] = [];
    const useCase = new ToggleSectionUseCase(repository(calls));

    await useCase.execute('traces', 'section-1', false);
    await useCase.execute('traces', 'section-1', true);

    expect(calls).toEqual(['complete:traces/section-1', 'reopen:traces/section-1']);
  });
});
