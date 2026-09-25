import type { Box, FlagKind } from '../models/Box';
import { AppError } from '../errors/AppError';
import type { BoxRepository, FlagSubmission } from '../repositories/BoxRepository';
import { normalizeFlag, validateFlag } from '../validation/flag';

export class ListBoxesUseCase {
  constructor(private readonly boxes: BoxRepository) {}

  execute(): Promise<Box[]> {
    return this.boxes.list();
  }
}

export class GetBoxUseCase {
  constructor(private readonly boxes: BoxRepository) {}

  execute(slug: string): Promise<Box> {
    return this.boxes.get(slug);
  }
}

/**
 * Soumet un flag. La saisie est vérifiée et normalisée ici : le serveur ne
 * voit jamais d'espace ni d'habillage, et une saisie évidemment fausse ne
 * part pas sur le réseau.
 */
export class SubmitFlagUseCase {
  constructor(private readonly boxes: BoxRepository) {}

  execute(slug: string, kind: FlagKind, flag: string): Promise<FlagSubmission> {
    const error = validateFlag(flag);
    if (error) return Promise.reject(AppError.validation({ flag: error }));
    return this.boxes.submitFlag(slug, kind, normalizeFlag(flag));
  }
}
