import type { BoxDraft } from '../models/Admin';

/**
 * Règles de saisie d'une machine. Miroir de celles du serveur, qui reste
 * l'autorité ; ici, elles évitent un aller-retour pour une erreur évidente.
 */
const FLAG_FORMAT = /^[0-9a-f]{32}$/;
const ADDRESS_FORMAT = /^[a-zA-Z0-9.:_-]{3,45}$/;

export interface BoxDraftErrors {
  readonly name?: string;
  readonly ipAddress?: string;
  readonly userFlag?: string;
  readonly rootFlag?: string;
}

export function validateBoxDraft(draft: BoxDraft): BoxDraftErrors {
  return {
    name: !draft.name.trim() ? 'Le nom est obligatoire.' : undefined,
    ipAddress: !ADDRESS_FORMAT.test(draft.ipAddress.trim())
      ? "L'adresse de la machine dans le réseau du lab est obligatoire."
      : undefined,
    userFlag: flagError(draft.userFlag),
    rootFlag: flagError(draft.rootFlag),
  };
}

export function boxDraftHasErrors(errors: BoxDraftErrors): boolean {
  return Object.values(errors).some(Boolean);
}

/** Un flag vide est accepté : tiré au hasard à la création, inchangé sinon. */
function flagError(flag: string): string | undefined {
  const value = flag.trim().toLowerCase();
  if (!value) return undefined;
  return FLAG_FORMAT.test(value) ? undefined : 'Un flag est une suite de 32 caractères hexadécimaux.';
}
