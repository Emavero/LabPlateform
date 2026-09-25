import type { AxiosInstance } from 'axios';
import type { LeaderboardEntry, PlayerProgress } from '@/domain/models/Progress';
import type { ScoreboardRepository } from '@/domain/repositories/ScoreboardRepository';
import { toLeaderboard, toProgress, type LeaderboardDto, type ProgressDto } from './mappers';

export class HttpScoreboardRepository implements ScoreboardRepository {
  constructor(private readonly http: AxiosInstance) {}

  async progress(): Promise<PlayerProgress> {
    const { data } = await this.http.get<ProgressDto>('/scoreboard/me');
    return toProgress(data);
  }

  async leaderboard(limit?: number): Promise<LeaderboardEntry[]> {
    const { data } = await this.http.get<LeaderboardDto>('/scoreboard', { params: limit ? { limit } : undefined });
    return toLeaderboard(data);
  }
}
