import type { User } from '../models/User';

export interface Credentials {
  readonly email: string;
  readonly password: string;
}

export interface Registration extends Credentials {
  readonly confirmPassword: string;
}

export interface PasswordResetRequestResult {
  readonly message: string;
  /** Renseigné uniquement quand le backend tourne en mode démo (pas de serveur d'e-mail). */
  readonly demoToken: string | null;
}

export interface PasswordReset {
  readonly token: string;
  readonly newPassword: string;
  readonly confirmPassword: string;
}

/** Contrat d'accès à l'authentification. La présentation n'en connaît que cette interface. */
export interface AuthRepository {
  register(registration: Registration): Promise<User>;
  login(credentials: Credentials): Promise<User>;
  logout(): Promise<void>;
  /** Utilisateur de la session en cours, ou null s'il n'y en a pas. */
  currentUser(): Promise<User | null>;
  requestPasswordReset(email: string): Promise<PasswordResetRequestResult>;
  resetPassword(reset: PasswordReset): Promise<void>;
}
