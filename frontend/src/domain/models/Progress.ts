export type Rank =
  | 'NOOB'
  | 'SCRIPT_KIDDIE'
  | 'HACKER'
  | 'PRO_HACKER'
  | 'ELITE_HACKER'
  | 'GURU'
  | 'OMNISCIENT';

/** Progression du joueur connecté, telle que le serveur la calcule. */
export interface PlayerProgress {
  readonly points: number;
  readonly availablePoints: number;
  readonly ownedFlags: number;
  readonly totalFlags: number;
  readonly boxesPwned: number;
  readonly firstBloods: number;
  readonly rank: Rank;
  readonly rankName: string;
  readonly nextRank: Rank | null;
  readonly nextRankName: string | null;
  readonly pointsToNextRank: number;
  /** Part du catalogue possédée, entre 0 et 1. */
  readonly completion: number;
}

export interface LeaderboardEntry {
  readonly position: number;
  readonly handle: string;
  readonly points: number;
  readonly ownedFlags: number;
  readonly firstBloods: number;
  readonly rank: Rank;
  readonly rankName: string;
  /** Ligne du joueur connecté, mise en avant dans le classement. */
  readonly self: boolean;
}

export const EMPTY_PROGRESS: PlayerProgress = {
  points: 0,
  availablePoints: 0,
  ownedFlags: 0,
  totalFlags: 0,
  boxesPwned: 0,
  firstBloods: 0,
  rank: 'NOOB',
  rankName: 'Noob',
  nextRank: null,
  nextRankName: null,
  pointsToNextRank: 0,
  completion: 0,
};

/** Avancement vers le rang suivant, entre 0 et 1. Vaut 1 au dernier rang. */
export function progressToNextRank(progress: PlayerProgress): number {
  if (!progress.nextRank) return 1;
  const target = progress.points + progress.pointsToNextRank;
  return target === 0 ? 0 : Math.min(progress.points / target, 1);
}

export function completionPercent(progress: PlayerProgress): number {
  return Math.round(progress.completion * 100);
}
