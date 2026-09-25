export type AppErrorKind =
  | 'validation'
  | 'unauthorized'
  | 'not_found'
  | 'conflict'
  | 'network'
  | 'unexpected';

/**
 * Erreur unique manipulée par la présentation, quelle que soit sa source
 * (règle métier locale, réponse HTTP, panne réseau).
 */
export class AppError extends Error {
  readonly kind: AppErrorKind;
  readonly fieldErrors: Readonly<Record<string, string>>;

  constructor(kind: AppErrorKind, message: string, fieldErrors: Record<string, string> = {}) {
    super(message);
    this.name = 'AppError';
    this.kind = kind;
    this.fieldErrors = fieldErrors;
  }

  static validation(fieldErrors: Record<string, string>): AppError {
    const first = Object.values(fieldErrors)[0] ?? 'Certains champs sont invalides.';
    return new AppError('validation', first, fieldErrors);
  }
}

export function toAppError(error: unknown): AppError {
  if (error instanceof AppError) return error;
  return new AppError('unexpected', 'Une erreur inattendue est survenue. Réessayez.');
}
