/**
 * Règles de saisie partagées par l'inscription, la réinitialisation et le
 * changement de mot de passe. Miroir des règles du domaine backend, pour
 * répondre immédiatement sans aller-retour réseau ; le serveur reste l'autorité.
 */
export const PASSWORD_MIN_LENGTH = 8;
export const PASSWORD_MAX_LENGTH = 128;

const EMAIL_FORMAT = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

export type FieldErrors<K extends string> = Partial<Record<K, string>>;

export function validateEmail(email: string): string | undefined {
  const value = email.trim();
  if (!value) return "L'adresse e-mail est obligatoire.";
  if (value.length > 254 || !EMAIL_FORMAT.test(value)) return "Le format de l'adresse e-mail est invalide.";
  return undefined;
}

export function validateNewPassword(
  password: string,
  confirmation: string,
): FieldErrors<'password' | 'confirmPassword'> {
  const errors: FieldErrors<'password' | 'confirmPassword'> = {};
  if (password.length < PASSWORD_MIN_LENGTH) {
    errors.password = `Au moins ${PASSWORD_MIN_LENGTH} caractères.`;
  } else if (password.length > PASSWORD_MAX_LENGTH) {
    errors.password = `Au plus ${PASSWORD_MAX_LENGTH} caractères.`;
  }
  if (!confirmation) {
    errors.confirmPassword = 'Confirmez le mot de passe.';
  } else if (password !== confirmation) {
    errors.confirmPassword = 'Les deux mots de passe ne correspondent pas.';
  }
  return errors;
}

export function hasErrors(errors: Record<string, string | undefined>): boolean {
  return Object.values(errors).some(Boolean);
}

/** Retire les entrées vides pour obtenir un dictionnaire exploitable par AppError. */
export function compact(errors: Record<string, string | undefined>): Record<string, string> {
  return Object.fromEntries(Object.entries(errors).filter((entry): entry is [string, string] => Boolean(entry[1])));
}
