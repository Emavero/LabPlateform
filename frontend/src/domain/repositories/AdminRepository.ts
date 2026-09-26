import type { AdminOverview, BoxDraft, CourseDraft, PublishedBox } from '../models/Admin';
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
}
