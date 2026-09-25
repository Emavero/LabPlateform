import { describe, expect, it } from 'vitest';
import { AppError } from '../errors/AppError';
import type { User } from '../models/User';
import type { AuthRepository, Registration } from '../repositories/AuthRepository';
import { LoginUseCase, RegisterUseCase, ResetPasswordUseCase } from './auth';

const alice: User = { id: 1, email: 'alice@example.com', role: 'USER' };

/** Faux dépôt : prouve que les cas d'usage ne dépendent que de l'interface. */
function fakeAuth(calls: string[] = []): AuthRepository {
  return {
    register: async (r: Registration) => {
      calls.push(`register:${r.email}`);
      return { ...alice, email: r.email };
    },
    login: async (c) => {
      calls.push(`login:${c.email}`);
      return alice;
    },
    logout: async () => undefined,
    currentUser: async () => null,
    requestPasswordReset: async () => ({ message: 'ok', demoToken: null }),
    resetPassword: async () => undefined,
  };
}

describe('RegisterUseCase', () => {
  it('rejects invalid input without calling the repository', async () => {
    const calls: string[] = [];
    const register = new RegisterUseCase(fakeAuth(calls));

    const error = await register
      .execute({ email: 'not-an-email', password: 'short', confirmPassword: 'other' })
      .catch((e: unknown) => e);

    expect(error).toBeInstanceOf(AppError);
    expect((error as AppError).kind).toBe('validation');
    expect(Object.keys((error as AppError).fieldErrors).sort()).toEqual(['confirmPassword', 'email', 'password']);
    expect(calls).toEqual([]);
  });

  it('trims the e-mail before registering', async () => {
    const calls: string[] = [];
    await new RegisterUseCase(fakeAuth(calls)).execute({
      email: '  bob@example.com ',
      password: 'password123',
      confirmPassword: 'password123',
    });
    expect(calls).toEqual(['register:bob@example.com']);
  });
});

describe('LoginUseCase', () => {
  it('requires both fields', async () => {
    const error = (await new LoginUseCase(fakeAuth())
      .execute({ email: '', password: '' })
      .catch((e: unknown) => e)) as AppError;
    expect(error.fieldErrors).toHaveProperty('email');
    expect(error.fieldErrors).toHaveProperty('password');
  });
});

describe('ResetPasswordUseCase', () => {
  it('maps password errors onto the reset form fields', async () => {
    const error = (await new ResetPasswordUseCase(fakeAuth())
      .execute({ token: 'abc', newPassword: 'short', confirmPassword: 'short' })
      .catch((e: unknown) => e)) as AppError;
    expect(Object.keys(error.fieldErrors)).toEqual(['newPassword']);
  });
});
