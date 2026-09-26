import type { AxiosInstance } from 'axios';
import type { AdminOverview, BoxDraft, CourseDraft, PublishedBox, UploadedMedia } from '@/domain/models/Admin';
import type { Course } from '@/domain/models/Course';
import type { AdminRepository } from '@/domain/repositories/AdminRepository';
import { toCourse, type CourseDto } from './mappers';

type PublishedBoxDto = Omit<PublishedBox, 'releasedAt'> & { releasedAt: string };

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

  async listBoxes(): Promise<PublishedBox[]> {
    const { data } = await this.http.get<PublishedBoxDto[]>('/admin/boxes');
    return data.map(toPublishedBox);
  }

  async createBox(draft: BoxDraft): Promise<PublishedBox> {
    const { data } = await this.http.post<PublishedBoxDto>('/admin/boxes', boxBody(draft));
    return toPublishedBox(data);
  }

  async updateBox(slug: string, draft: BoxDraft): Promise<PublishedBox> {
    const { data } = await this.http.put<PublishedBoxDto>(`/admin/boxes/${encodeURIComponent(slug)}`, boxBody(draft));
    return toPublishedBox(data);
  }

  async deleteBox(slug: string): Promise<void> {
    await this.http.delete(`/admin/boxes/${encodeURIComponent(slug)}`);
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

  async getCourse(slug: string): Promise<Course> {
    const { data } = await this.http.get<CourseDto>(`/admin/courses/${encodeURIComponent(slug)}`);
    return toCourse(data);
  }

  /** Multipart : le navigateur pose lui-même la frontière du corps. */
  async uploadMedia(file: File): Promise<UploadedMedia> {
    const form = new FormData();
    form.append('file', file);
    const { data } = await this.http.post<UploadedMedia>('/admin/media', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 600_000,
    });
    return { ...data };
  }
}

function toPublishedBox(dto: PublishedBoxDto): PublishedBox {
  return { ...dto, releasedAt: new Date(dto.releasedAt) };
}

/** Un flag vide veut dire « ne change rien » : on l'envoie absent. */
function boxBody(draft: BoxDraft) {
  return {
    name: draft.name.trim(),
    operatingSystem: draft.operatingSystem,
    difficulty: draft.difficulty,
    synopsis: draft.synopsis.trim(),
    ipAddress: draft.ipAddress.trim(),
    maker: draft.maker.trim(),
    retired: draft.retired,
    userFlag: draft.userFlag.trim() || null,
    rootFlag: draft.rootFlag.trim() || null,
  };
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
      questions: section.questions.map((question) => ({
        statement: question.statement.trim(),
        choices: question.choices.map((choice) => ({ label: choice.label.trim(), correct: choice.correct })),
      })),
    })),
  };
}
