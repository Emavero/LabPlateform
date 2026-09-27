import { isLanguage, type Language } from '@/domain/models/Language';

const STORAGE_KEY = 'cybermans.language';

/**
 * Langue retenue sur cet appareil.
 * <p>
 * Ce module existe pour que deux mondes s'accordent sans se connaître : la
 * présentation, qui écrit le choix, et le client HTTP — créé hors de React —
 * qui doit l'annoncer au serveur dans {@code Accept-Language}. Passer par le
 * stockage plutôt que par une variable partagée évite un état global de plus.
 * <p>
 * Chaque accès est protégé : en navigation privée, certains navigateurs lèvent
 * au lieu de rendre une valeur vide. Une langue oubliée n'est pas une panne, la
 * page retombe alors sur le français.
 */
export function readStoredLanguage(): Language | null {
  try {
    const stored = window.localStorage.getItem(STORAGE_KEY);
    return isLanguage(stored) ? stored : null;
  } catch {
    return null;
  }
}

export function storeLanguage(language: Language): void {
  try {
    window.localStorage.setItem(STORAGE_KEY, language);
  } catch {
    // Stockage refusé : la langue vaut pour cette session, ce qui suffit.
  }
}

/** Langue du navigateur, si nous la servons. */
export function browserLanguage(): Language | null {
  const preferred = typeof navigator === 'undefined' ? '' : navigator.language.slice(0, 2).toLowerCase();
  return isLanguage(preferred) ? preferred : null;
}
