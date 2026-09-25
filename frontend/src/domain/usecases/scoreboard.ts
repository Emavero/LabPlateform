import type { LeaderboardEntry, PlayerProgress } from '../models/Progress';
import type { ScoreboardRepository } from '../repositories/ScoreboardRepository';

export class GetProgressUseCase {
  constructor(private readonly scoreboard: ScoreboardRepository) {}

  execute(): Promise<PlayerProgress> {
    return this.scoreboard.progress();
  }
}

export class GetLeaderboardUseCase {
  constructor(private readonly scoreboard: ScoreboardRepository) {}

  execute(limit?: number): Promise<LeaderboardEntry[]> {
    return this.scoreboard.leaderboard(limit);
  }
}
