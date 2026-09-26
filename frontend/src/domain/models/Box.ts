import type { OperatingSystem } from './VirtualMachine';

export type Difficulty = 'VERY_EASY' | 'EASY' | 'MEDIUM' | 'HARD' | 'INSANE';
export type FlagKind = 'USER' | 'ROOT';

/**
 * Machine du catalogue : une cible partagée à compromettre, à ne pas
 * confondre avec la machine d'attaque personnelle (VirtualMachine).
 */
export interface Box {
  readonly slug: string;
  readonly name: string;
  readonly os: OperatingSystem;
  readonly osName: string;
  readonly difficulty: Difficulty;
  readonly difficultyName: string;
  readonly userFlagPoints: number;
  readonly rootFlagPoints: number;
  readonly totalPoints: number;
  readonly synopsis: string;
  readonly ipAddress: string;
  readonly maker: string;
  readonly releasedAt: Date;
  readonly retired: boolean;
  readonly userOwned: boolean;
  readonly rootOwned: boolean;
  readonly pwned: boolean;
  readonly firstBlood: boolean;
  readonly pointsEarned: number;
  readonly lastOwnedAt: Date | null;
  /** Difficulté ressentie par ceux qui ont fait la machine. */
  readonly ratingVotes: number;
  readonly ratingAverage: number;
  readonly perceivedDifficulty: Difficulty | null;
  readonly perceivedDifficultyName: string | null;
  /** Note donnée par le joueur connecté, nulle s'il n'a pas voté. */
  readonly myRating: Difficulty | null;
}

/** Ordre d'affichage des difficultés, du plus abordable au plus exigeant. */
export const DIFFICULTY_ORDER: readonly Difficulty[] = ['VERY_EASY', 'EASY', 'MEDIUM', 'HARD', 'INSANE'];

export const FLAG_LABELS: Record<FlagKind, string> = {
  USER: 'Flag utilisateur',
  ROOT: 'Flag root',
};

/** Position de la difficulté sur l'échelle, de 1 à 5 : sert à la jauge. */
export function difficultyLevel(difficulty: Difficulty): number {
  return DIFFICULTY_ORDER.indexOf(difficulty) + 1;
}

export function isOwned(box: Box, kind: FlagKind): boolean {
  return kind === 'USER' ? box.userOwned : box.rootOwned;
}

export function pointsOf(box: Box, kind: FlagKind): number {
  return kind === 'USER' ? box.userFlagPoints : box.rootFlagPoints;
}

/** Flags restant à valider, dans l'ordre où on les obtient sur la machine. */
export function remainingFlags(box: Box): FlagKind[] {
  return (['USER', 'ROOT'] as const).filter((kind) => !isOwned(box, kind));
}

/** Écart entre la difficulté annoncée et celle ressentie, en paliers. */
export function ratingGap(box: Box): number {
  if (!box.perceivedDifficulty) return 0;
  return DIFFICULTY_ORDER.indexOf(box.perceivedDifficulty) - DIFFICULTY_ORDER.indexOf(box.difficulty);
}

export type BoxFilter = 'ALL' | 'TODO' | 'PWNED';

export const BOX_FILTER_LABELS: Record<BoxFilter, string> = {
  ALL: 'Toutes',
  TODO: 'À faire',
  PWNED: 'Possédées',
};

export function matchesFilter(box: Box, filter: BoxFilter): boolean {
  if (filter === 'PWNED') return box.pwned;
  if (filter === 'TODO') return !box.pwned;
  return true;
}
