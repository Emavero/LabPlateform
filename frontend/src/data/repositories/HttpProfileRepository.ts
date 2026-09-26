import type { AxiosInstance } from 'axios';
import type { Achievement, ActivityEntry } from '@/domain/models/Profile';
import type { ProfileRepository } from '@/domain/repositories/ProfileRepository';
import { toAchievement, toActivityEntry, type AchievementDto, type ActivityDto } from './mappers';

export class HttpProfileRepository implements ProfileRepository {
  constructor(private readonly http: AxiosInstance) {}

  async achievements(): Promise<Achievement[]> {
    const { data } = await this.http.get<AchievementDto[]>('/profile/achievements');
    return data.map(toAchievement);
  }

  async activity(limit?: number): Promise<ActivityEntry[]> {
    const { data } = await this.http.get<ActivityDto[]>('/profile/activity', {
      params: limit ? { limit } : undefined,
    });
    return data.map(toActivityEntry);
  }
}
