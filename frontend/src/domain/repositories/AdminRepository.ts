import type { AdminOverview, CourseDraft } from '../models/Admin';
import type { Course } from '../models/Course';

export interface AdminRepository {
  overview(): Promise<AdminOverview>;
  createCourse(draft: CourseDraft): Promise<Course>;
  updateCourse(slug: string, draft: CourseDraft): Promise<Course>;
  deleteCourse(slug: string): Promise<void>;
}
