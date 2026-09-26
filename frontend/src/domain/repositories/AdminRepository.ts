import type { AdminOverview, BoxDraft, CourseDraft, PublishedBox, UploadedMedia } from '../models/Admin';
import type { Course } from '../models/Course';

export interface AdminRepository {
  overview(): Promise<AdminOverview>;
  listBoxes(): Promise<PublishedBox[]>;
  createBox(draft: BoxDraft): Promise<PublishedBox>;
  updateBox(slug: string, draft: BoxDraft): Promise<PublishedBox>;
  deleteBox(slug: string): Promise<void>;
  createCourse(draft: CourseDraft): Promise<Course>;
  updateCourse(slug: string, draft: CourseDraft): Promise<Course>;
  deleteCourse(slug: string): Promise<void>;
  uploadMedia(file: File): Promise<UploadedMedia>;
  /** Fiche complète d'un cours, bonnes réponses comprises. */
  getCourse(slug: string): Promise<Course>;
}
