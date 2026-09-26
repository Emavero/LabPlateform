import { AppError } from '../errors/AppError';
import { validateWriteup, type Writeup, type WriteupDraft } from '../models/Writeup';
import type { WriteupRepository } from '../repositories/WriteupRepository';

export class ListWriteupsUseCase {
  constructor(private readonly writeups: WriteupRepository) {}

  execute(slug: string): Promise<Writeup[]> {
    return this.writeups.list(slug);
  }
}

/** Enregistre le compte rendu du joueur, après vérification de la saisie. */
export class SaveWriteupUseCase {
  constructor(private readonly writeups: WriteupRepository) {}

  execute(slug: string, draft: WriteupDraft): Promise<Writeup> {
    const errors = validateWriteup(draft);
    const present = Object.fromEntries(
      Object.entries(errors).filter((entry): entry is [string, string] => Boolean(entry[1])),
    );
    if (Object.keys(present).length > 0) return Promise.reject(AppError.validation(present));
    return this.writeups.save(slug, draft);
  }
}

export class DeleteWriteupUseCase {
  constructor(private readonly writeups: WriteupRepository) {}

  execute(slug: string): Promise<void> {
    return this.writeups.remove(slug);
  }
}
