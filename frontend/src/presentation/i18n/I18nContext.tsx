import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { en } from './en';
import { fr } from './fr';
import { browserLanguage, readStoredLanguage, storeLanguage } from '@/data/storage/languagePreference';
import { LOCALES, type Language } from '@/domain/models/Language';
import { translateMessage } from './messages';
import { interpolate, type Vars } from './types';
import type { CatalogueKey as Key } from './fr';

const CATALOGUES: Record<Language, Record<Key, string>> = { fr, en };

interface I18nState {
  language: Language;
  setLanguage: (language: Language) => void;
  /** Texte traduit, trous remplis. */
  t: (key: Key, vars?: Vars) => string;
  /**
   * Message d'erreur traduit. Ceux-là arrivent déjà formulés en français,
   * depuis le domaine ou le serveur : ils se traduisent par leur texte.
   */
  tm: (message: string) => string;
  /** Étiquette locale d'une date : le format d'une date n'est pas le même partout. */
  formatDate: (date: Date) => string;
  formatDateTime: (date: Date) => string;
  formatTime: (date: Date) => string;
  /** Code de langue à passer aux fonctions de formatage (`Intl`, `toLocaleString`). */
  locale: string;
}

const I18nContext = createContext<I18nState | null>(null);

/**
 * Langue de départ : celle qui a été choisie sur cet appareil, sinon celle du
 * navigateur si nous la servons, sinon le français — la langue de référence.
 */
function initialLanguage(): Language {
  return readStoredLanguage() ?? browserLanguage() ?? 'fr';
}

/**
 * Langue de l'interface.
 * <p>
 * Le choix est retenu sur l'appareil, et non dans le compte : c'est un confort
 * de lecture, pas une donnée de profil, et il doit valoir avant même d'être
 * connecté — sur la page de connexion, par exemple.
 */
export function I18nProvider({ children }: { children: ReactNode }) {
  const [language, setLanguageState] = useState<Language>(initialLanguage);

  const setLanguage = useCallback((next: Language) => {
    setLanguageState(next);
    storeLanguage(next);
  }, []);

  useEffect(() => {
    // Le document porte sa langue : les lecteurs d'écran prononcent alors le
    // texte correctement, et le navigateur propose la bonne traduction.
    document.documentElement.lang = language;
  }, [language]);

  const value = useMemo<I18nState>(() => {
    const catalogue = CATALOGUES[language];
    const locale = LOCALES[language];
    return {
      language,
      setLanguage,
      locale,
      // Clé absente : on rend la clé elle-même plutôt qu'une page vide, et le
      // typage empêche de toute façon d'en écrire une qui n'existe pas.
      t: (key, vars) => interpolate(catalogue[key] ?? String(key), vars),
      tm: (message) => translateMessage(message, language),
      formatDate: (date) => date.toLocaleDateString(locale, { dateStyle: 'long' }),
      formatDateTime: (date) => date.toLocaleString(locale, { dateStyle: 'medium', timeStyle: 'short' }),
      formatTime: (date) => date.toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' }),
    };
  }, [language, setLanguage]);

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n(): I18nState {
  const state = useContext(I18nContext);
  if (!state) throw new Error('useI18n doit être utilisé sous <I18nProvider>.');
  return state;
}

export type { Key as CatalogueKey };
