import type { UserProfile } from '../models/User';

export interface PasswordChange {
  readonly currentPassword: string;
  readonly newPassword: string;
  readonly confirmPassword: string;
}

export interface AccountRepository {
  getProfile(): Promise<UserProfile>;
  changePassword(change: PasswordChange): Promise<void>;
}
