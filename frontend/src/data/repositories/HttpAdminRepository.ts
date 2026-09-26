import type { AxiosInstance } from 'axios';
import type { AdminOverview, CourseDraft } from '@/domain/models/Admin';
import type { Course } from '@/domain/models/Course';
import type { AdminRepository } from '@/domain/repositories/AdminRepository';
import { toCourse, type CourseDto } from './mappers';

interface AdminOverviewDto {
  users: number;
  boxes: number;
  courses: number;
  sections: number;
  flagsValidated: number;
  sectionsCompleted: number;
}

export class HttpAdminRepository implements AdminRepository {
  constructor(private readonly http: AxiosInstance) {}

  async overview(): Promise<AdminOverview> {
    const { data } = await this.http.get<AdminOverviewDto>('/admin/overview');
    return { ...data };
  }

  async createCourse(draft: CourseDraft): Promise<Course> {
    const { data } = await this.http.post<CourseDto>('/admin/courses', body(draft));
    return toCourse(data);
  }

  async updateCourse(slug: string, draft: CourseDraft): Promise<Course> {
    const { data } = await this.http.put<CourseDto>(`/admin/courses/${encodeURIComponent(slug)}`, body(draft));
    return toCourse(data);
  }

  async deleteCourse(slug: string): Promise<void> {
    await this.http.delete(`/admin/courses/${encodeURIComponent(slug)}`);
  }
}

/** Le serveur attend une vidéo absente plutôt qu'une chaîne vide. */
function body(draft: CourseDraft) {
  return {
    title: draft.title.trim(),
    track: draft.track,
    level: draft.level,
    summary: draft.summary.trim(),
    sections: draft.sections.map((section) => ({
      id: section.id,
      title: section.title.trim(),
      kind: section.kind,
      minutes: section.minutes,
      content: section.content,
      videoUrl: section.videoUrl.trim() || null,
    })),
  };
}
