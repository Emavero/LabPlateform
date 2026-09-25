import type { AxiosInstance } from 'axios';
import type { UserProfile } from '@/domain/models/User';
import type { AccountRepository, PasswordChange } from '@/domain/repositories/AccountRepository';
import { toUserProfile, type ProfileDto } from './mappers';

export class HttpAccountRepository implements AccountRepository {
  constructor(private readonly http: AxiosInstance) {}

  async getProfile(): Promise<UserProfile> {
    const { data } = await this.http.get<ProfileDto>('/users/me');
    return toUserProfile(data);
  }

  async changePassword(change: PasswordChange): Promise<void> {
    await this.http.put('/users/me/password', change);
  }
}
