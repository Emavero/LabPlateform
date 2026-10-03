import { isTheme, type Theme } from '@/domain/models/Theme';

const STORAGE_KEY = 'cybermans.theme';

/**
 * Thème retenu sur cet appareil.
 * <p>
 * Même choix que pour la langue : un confort de lecture appartient à
 * l'appareil, pas au compte — le thème doit valoir avant même d'être
 * connecté, et sur un poste partagé chacun garde le sien.
 * <p>
 * Chaque accès est protégé : en navigation privée, certains navigateurs lèvent
 * au lieu de rendre une valeur vide. Un thème oublié n'est pas une panne, la
 * page retombe alors sur le thème sombre.
 */
export function readStoredTheme(): Theme | null {
  try {
    const stored = window.localStorage.getItem(STORAGE_KEY);
    return isTheme(stored) ? stored : null;
  } catch {
    return null;
  }
}

export function storeTheme(theme: Theme): void {
  try {
    window.localStorage.setItem(STORAGE_KEY, theme);
  } catch {
    // Stockage refusé : le thème vaut pour cette session, ce qui suffit.
  }
}
