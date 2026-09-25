/**
 * Règle de saisie d'un flag. Miroir de domain/box/Flag côté serveur, pour
 * refuser une saisie manifestement fausse sans aller-retour ; le serveur
 * reste l'autorité, lui seul connaît la bonne valeur.
 */
const FLAG_FORMAT = /^[0-9a-f]{32}$/;

export const FLAG_LENGTH = 32;

/** Retire les espaces, la casse et un éventuel habillage « CYBM{…} ». */
export function normalizeFlag(raw: string): string {
  const trimmed = raw.trim().toLowerCase();
  const opening = trimmed.indexOf('{');
  const closing = trimmed.lastIndexOf('}');
  return opening >= 0 && closing > opening ? trimmed.slice(opening + 1, closing).trim() : trimmed;
}

export function validateFlag(raw: string): string | undefined {
  if (!raw.trim()) return 'Le flag est obligatoire.';
  if (!FLAG_FORMAT.test(normalizeFlag(raw))) return `Un flag est une suite de ${FLAG_LENGTH} caractères hexadécimaux.`;
  return undefined;
}
