import { AppError } from '../errors/AppError';
import type { User } from '../models/User';
import type {
  AuthRepository,
  Credentials,
  PasswordReset,
  PasswordResetRequestResult,
  Registration,
} from '../repositories/AuthRepository';
import { compact, hasErrors, validateEmail, validateNewPassword } from '../validation/credentials';

export class LoginUseCase {
  constructor(private readonly auth: AuthRepository) {}

  execute(credentials: Credentials): Promise<User> {
    const errors = {
      email: credentials.email.trim() ? undefined : "L'adresse e-mail est obligatoire.",
      password: credentials.password ? undefined : 'Le mot de passe est obligatoire.',
    };
    if (hasErrors(errors)) return Promise.reject(AppError.validation(compact(errors)));
    return this.auth.login({ email: credentials.email.trim(), password: credentials.password });
  }
}

export class RegisterUseCase {
  constructor(private readonly auth: AuthRepository) {}

  execute(registration: Registration): Promise<User> {
    const errors = {
      email: validateEmail(registration.email),
      ...validateNewPassword(registration.password, registration.confirmPassword),
    };
    if (hasErrors(errors)) return Promise.reject(AppError.validation(compact(errors)));
    return this.auth.register({ ...registration, email: registration.email.trim() });
  }
}

export class LogoutUseCase {
  constructor(private readonly auth: AuthRepository) {}

  execute(): Promise<void> {
    return this.auth.logout();
  }
}

export class RestoreSessionUseCase {
  constructor(private readonly auth: AuthRepository) {}

  execute(): Promise<User | null> {
    return this.auth.currentUser();
  }
}

export class RequestPasswordResetUseCase {
  constructor(private readonly auth: AuthRepository) {}

  execute(email: string): Promise<PasswordResetRequestResult> {
    const error = validateEmail(email);
    if (error) return Promise.reject(AppError.validation({ email: error }));
    return this.auth.requestPasswordReset(email.trim());
  }
}

export class ResetPasswordUseCase {
  constructor(private readonly auth: AuthRepository) {}

  execute(reset: PasswordReset): Promise<void> {
    const errors = {
      token: reset.token.trim() ? undefined : 'Le lien de réinitialisation est incomplet.',
      ...renamePasswordErrors(validateNewPassword(reset.newPassword, reset.confirmPassword)),
    };
    if (hasErrors(errors)) return Promise.reject(AppError.validation(compact(errors)));
    return this.auth.resetPassword({ ...reset, token: reset.token.trim() });
  }
}

function renamePasswordErrors(errors: { password?: string; confirmPassword?: string }) {
  return { newPassword: errors.password, confirmPassword: errors.confirmPassword };
}
