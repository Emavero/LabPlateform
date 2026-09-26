import type { Achievement, ActivityEntry } from '../models/Profile';

export interface ProfileRepository {
  achievements(): Promise<Achievement[]>;
  activity(limit?: number): Promise<ActivityEntry[]>;
}
