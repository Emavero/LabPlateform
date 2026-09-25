import type { LeaderboardEntry, PlayerProgress } from '../models/Progress';

export interface ScoreboardRepository {
  progress(): Promise<PlayerProgress>;
  leaderboard(limit?: number): Promise<LeaderboardEntry[]>;
}
