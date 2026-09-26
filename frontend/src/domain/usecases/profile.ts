import type { Achievement, ActivityEntry } from '../models/Profile';
import type { ProfileRepository } from '../repositories/ProfileRepository';

export class GetAchievementsUseCase {
  constructor(private readonly profile: ProfileRepository) {}

  execute(): Promise<Achievement[]> {
    return this.profile.achievements();
  }
}

export class GetActivityUseCase {
  constructor(private readonly profile: ProfileRepository) {}

  execute(limit?: number): Promise<ActivityEntry[]> {
    return this.profile.activity(limit);
  }
}
