import { AppError } from '../errors/AppError';
import type { UserProfile } from '../models/User';
import type { AccountRepository, PasswordChange } from '../repositories/AccountRepository';
import { compact, hasErrors, validateNewPassword } from '../validation/credentials';

export class GetProfileUseCase {
  constructor(private readonly account: AccountRepository) {}

  execute(): Promise<UserProfile> {
    return this.account.getProfile();
  }
}

export class ChangePasswordUseCase {
  constructor(private readonly account: AccountRepository) {}

  execute(change: PasswordChange): Promise<void> {
    const passwordErrors = validateNewPassword(change.newPassword, change.confirmPassword);
    const errors = {
      currentPassword: change.currentPassword ? undefined : 'Saisissez votre mot de passe actuel.',
      newPassword: passwordErrors.password,
      confirmPassword: passwordErrors.confirmPassword,
    };
    if (hasErrors(errors)) return Promise.reject(AppError.validation(compact(errors)));
    return this.account.changePassword(change);
  }
}
