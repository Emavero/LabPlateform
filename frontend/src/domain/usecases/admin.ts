import { AppError } from '../errors/AppError';
import type { AdminOverview, CourseDraft } from '../models/Admin';
import type { Course } from '../models/Course';
import type { AdminRepository } from '../repositories/AdminRepository';
import { draftHasErrors, validateDraft } from '../validation/courseDraft';

export class GetAdminOverviewUseCase {
  constructor(private readonly admin: AdminRepository) {}

  execute(): Promise<AdminOverview> {
    return this.admin.overview();
  }
}

/**
 * Publie ou met à jour un cours. La saisie est vérifiée avant l'envoi : un
 * brouillon manifestement incomplet ne part pas sur le réseau.
 */
export class SaveCourseUseCase {
  constructor(private readonly admin: AdminRepository) {}

  execute(draft: CourseDraft, slug?: string): Promise<Course> {
    const errors = validateDraft(draft);
    if (draftHasErrors(errors)) {
      return Promise.reject(
        AppError.validation({
          ...(errors.title ? { title: errors.title } : {}),
          ...(errors.summary ? { summary: errors.summary } : {}),
          ...(errors.sections ? { sections: errors.sections } : {}),
          ...Object.fromEntries(
            Object.entries(errors.bySection).map(([index, message]) => [`section-${index}`, message]),
          ),
        }),
      );
    }
    return slug ? this.admin.updateCourse(slug, draft) : this.admin.createCourse(draft);
  }
}

export class DeleteCourseUseCase {
  constructor(private readonly admin: AdminRepository) {}

  execute(slug: string): Promise<void> {
    return this.admin.deleteCourse(slug);
  }
}
