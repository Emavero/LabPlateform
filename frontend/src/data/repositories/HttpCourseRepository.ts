import type { AxiosInstance } from 'axios';
import type { Course, CourseSummary, LearningProgress, Track } from '@/domain/models/Course';
import type { CourseRepository } from '@/domain/repositories/CourseRepository';
import {
  toCourse,
  toCourseSummary,
  toLearningProgress,
  toTrack,
  type CourseDto,
  type CourseSummaryDto,
  type LearningProgressDto,
  type TrackDto,
} from './mappers';

export class HttpCourseRepository implements CourseRepository {
  constructor(private readonly http: AxiosInstance) {}

  async tracks(): Promise<Track[]> {
    const { data } = await this.http.get<TrackDto[]>('/courses/tracks');
    return data.map(toTrack);
  }

  async list(trackSlug?: string): Promise<CourseSummary[]> {
    const { data } = await this.http.get<CourseSummaryDto[]>('/courses', {
      params: trackSlug ? { track: trackSlug } : undefined,
    });
    return data.map(toCourseSummary);
  }

  async get(slug: string): Promise<Course> {
    const { data } = await this.http.get<CourseDto>(`/courses/${encodeURIComponent(slug)}`);
    return toCourse(data);
  }

  async progress(): Promise<LearningProgress[]> {
    const { data } = await this.http.get<LearningProgressDto[]>('/courses/progress');
    return data.map(toLearningProgress);
  }

  async completeSection(courseSlug: string, sectionSlug: string): Promise<Course> {
    const { data } = await this.http.post<CourseDto>(this.completionPath(courseSlug, sectionSlug));
    return toCourse(data);
  }

  async reopenSection(courseSlug: string, sectionSlug: string): Promise<Course> {
    const { data } = await this.http.delete<CourseDto>(this.completionPath(courseSlug, sectionSlug));
    return toCourse(data);
  }

  private completionPath(courseSlug: string, sectionSlug: string): string {
    return `/courses/${encodeURIComponent(courseSlug)}/sections/${encodeURIComponent(sectionSlug)}/completion`;
  }
}
