import type { LabVpnProfile } from '../models/Vpn';
import { AppError } from '../errors/AppError';
import { mediaError, type AdminOverview, type BoxDraft, type CourseDraft, type PublishedBox, type UploadedMedia } from '../models/Admin';
import type { Course } from '../models/Course';
import type { AdminRepository } from '../repositories/AdminRepository';
import { boxDraftHasErrors, validateBoxDraft } from '../validation/boxDraft';
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

export class ListBoxesUseCase {
  constructor(private readonly admin: AdminRepository) {}

  execute(): Promise<PublishedBox[]> {
    return this.admin.listBoxes();
  }
}

/** Publie ou met à jour une machine, après vérification de la saisie. */
export class SaveBoxUseCase {
  constructor(private readonly admin: AdminRepository) {}

  execute(draft: BoxDraft, slug?: string): Promise<PublishedBox> {
    const errors = validateBoxDraft(draft);
    if (boxDraftHasErrors(errors)) {
      return Promise.reject(
        AppError.validation(
          Object.fromEntries(Object.entries(errors).filter((entry): entry is [string, string] => Boolean(entry[1]))),
        ),
      );
    }
    return slug ? this.admin.updateBox(slug, draft) : this.admin.createBox(draft);
  }
}

export class DeleteBoxUseCase {
  constructor(private readonly admin: AdminRepository) {}

  execute(slug: string): Promise<void> {
    return this.admin.deleteBox(slug);
  }
}

/** Téléverse une vidéo et renvoie l'adresse à référencer dans la section. */
export class UploadMediaUseCase {
  constructor(private readonly admin: AdminRepository) {}

  execute(file: File): Promise<UploadedMedia> {
    const error = mediaError(file);
    if (error) return Promise.reject(AppError.validation({ file: error }));
    return this.admin.uploadMedia(file);
  }
}

/** Lit un cours pour l'éditer : c'est la seule lecture qui révèle les bonnes réponses. */
export class GetCourseForEditingUseCase {
  constructor(private readonly admin: AdminRepository) {}

  execute(slug: string): Promise<Course> {
    return this.admin.getCourse(slug);
  }
}

/**
 * Profil VPN mis à disposition des apprenants.
 * <p>
 * Un seul fichier pour toute la plateforme : le déposer remplace le précédent,
 * et le retirer coupe le téléchargement pour tout le monde.
 */
export class GetLabVpnProfileUseCase {
  constructor(private readonly admin: AdminRepository) {}

  execute(): Promise<LabVpnProfile> {
    return this.admin.getVpnProfile();
  }
}

export class UploadLabVpnProfileUseCase {
  constructor(private readonly admin: AdminRepository) {}

  execute(file: File): Promise<LabVpnProfile> {
    return this.admin.uploadVpnProfile(file);
  }
}

export class RemoveLabVpnProfileUseCase {
  constructor(private readonly admin: AdminRepository) {}

  execute(): Promise<void> {
    return this.admin.removeVpnProfile();
  }
}
