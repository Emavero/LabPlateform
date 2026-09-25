import type { AxiosInstance } from 'axios';
import { AppError } from '@/domain/errors/AppError';
import type { User } from '@/domain/models/User';
import type {
  AuthRepository,
  Credentials,
  PasswordReset,
  PasswordResetRequestResult,
  Registration,
} from '@/domain/repositories/AuthRepository';
import { toUser, type UserDto } from './mappers';

interface SessionDto {
  user: UserDto;
}

interface ForgotPasswordDto {
  message: string;
  devResetToken: string | null;
}

export class HttpAuthRepository implements AuthRepository {
  constructor(private readonly http: AxiosInstance) {}

  async register(registration: Registration): Promise<User> {
    const { data } = await this.http.post<SessionDto>('/auth/register', registration);
    return toUser(data.user);
  }

  async login(credentials: Credentials): Promise<User> {
    const { data } = await this.http.post<SessionDto>('/auth/login', credentials);
    return toUser(data.user);
  }

  async logout(): Promise<void> {
    await this.http.post('/auth/logout');
  }

  async currentUser(): Promise<User | null> {
    try {
      const { data } = await this.http.get<UserDto>('/users/me');
      return toUser(data);
    } catch (error) {
      if (error instanceof AppError && error.kind === 'unauthorized') return null;
      throw error;
    }
  }

  async requestPasswordReset(email: string): Promise<PasswordResetRequestResult> {
    const { data } = await this.http.post<ForgotPasswordDto>('/auth/forgot-password', { email });
    return { message: data.message, demoToken: data.devResetToken ?? null };
  }

  async resetPassword(reset: PasswordReset): Promise<void> {
    await this.http.post('/auth/reset-password', reset);
  }
}
