import type { OperatingSystem } from './VirtualMachine';

export type Difficulty = 'VERY_EASY' | 'EASY' | 'MEDIUM' | 'HARD' | 'INSANE';
export type InstanceStatus = 'STOPPED' | 'RUNNING';
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
  /** Nulle sur une machine verrouillée : le serveur ne l'envoie pas. */
  readonly ipAddress: string | null;
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
  /** Cible lancée par le joueur : arrêtée tant qu'il ne l'a pas demandée. */
  readonly instanceStatus: InstanceStatus;
  readonly instanceAddress: string | null;
  readonly instanceExpiresAt: Date | null;
  /** Machine réservée aux abonnés Pro. */
  readonly proOnly: boolean;
  /**
   * Réservée, et ce joueur n'y a pas droit. La fiche reste consultable pour
   * qu'il voie ce qu'il obtiendrait, mais son adresse et son synopsis ne sont
   * pas envoyés et toute action dessus est refusée par le serveur.
   */
  readonly locked: boolean;
}

export function isInstanceRunning(box: Box): boolean {
  return box.instanceStatus === 'RUNNING';
}

/** Minutes restantes avant l'extinction automatique, jamais négatif. */
export function minutesLeft(box: Box, now: Date = new Date()): number {
  if (!box.instanceExpiresAt) return 0;
  return Math.max(0, Math.ceil((box.instanceExpiresAt.getTime() - now.getTime()) / 60_000));
}

/** Ordre d'affichage des difficultés, du plus abordable au plus exigeant. */
export const DIFFICULTY_ORDER: readonly Difficulty[] = ['VERY_EASY', 'EASY', 'MEDIUM', 'HARD', 'INSANE'];

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

export function matchesFilter(box: Box, filter: BoxFilter): boolean {
  if (filter === 'PWNED') return box.pwned;
  if (filter === 'TODO') return !box.pwned;
  return true;
}
