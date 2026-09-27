/**
 * Langues servies par la plateforme.
 * <p>
 * Le français est la langue de référence : c'est en français que les textes
 * sont écrits, et l'anglais en est la traduction.
 */
export const LANGUAGES = ['fr', 'en'] as const;

export type Language = (typeof LANGUAGES)[number];

export const LANGUAGE_NAMES: Record<Language, string> = {
  fr: 'Français',
  en: 'English',
};

/** Code de formatage associé : `Intl` et `toLocaleString` en ont besoin. */
export const LOCALES: Record<Language, string> = {
  fr: 'fr-FR',
  en: 'en-GB',
};

export function isLanguage(candidate: string | null | undefined): candidate is Language {
  return candidate != null && (LANGUAGES as readonly string[]).includes(candidate);
}
