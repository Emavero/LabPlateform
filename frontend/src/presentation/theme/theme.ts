import { readStoredTheme } from '@/data/storage/themePreference';
import { DEFAULT_THEME, type Theme } from '@/domain/models/Theme';

/**
 * Thème de départ : celui choisi sur cet appareil, sinon le sombre.
 * <p>
 * La préférence système (`prefers-color-scheme`) n'est volontairement pas
 * consultée : le sombre est le thème de la plateforme, et la plupart des
 * postes sont configurés en clair — les suivre enverrait tout le monde dans un
 * thème que personne n'a demandé. Le clair reste à un clic, et ce choix-là est
 * retenu.
 */
export function initialTheme(): Theme {
  return readStoredTheme() ?? DEFAULT_THEME;
}

/**
 * Pose le thème sur la racine du document : toute la feuille de style en
 * dépend par `[data-theme]`.
 * <p>
 * Appelée avant le premier rendu, et non seulement depuis React : sans cela la
 * page s'afficherait en sombre le temps d'un battement avant de passer au
 * clair.
 */
export function applyTheme(theme: Theme): void {
  document.documentElement.dataset.theme = theme;
}
