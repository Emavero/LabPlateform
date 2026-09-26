import type { Box, Difficulty, FlagKind } from '../models/Box';
import type { PlayerProgress } from '../models/Progress';

/** Résultat d'une soumission acceptée par le serveur. */
export interface FlagSubmission {
  readonly slug: string;
  readonly name: string;
  readonly kind: FlagKind;
  readonly pointsAwarded: number;
  readonly firstBlood: boolean;
  readonly pwned: boolean;
  readonly progress: PlayerProgress;
}

export interface BoxRepository {
  list(): Promise<Box[]>;
  get(slug: string): Promise<Box>;
  submitFlag(slug: string, kind: FlagKind, flag: string): Promise<FlagSubmission>;
  /** Note de difficulté ressentie : réservée aux machines possédées. */
  rate(slug: string, difficulty: Difficulty): Promise<Box>;
}
