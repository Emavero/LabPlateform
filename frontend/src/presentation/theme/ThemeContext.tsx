import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { storeTheme } from '@/data/storage/themePreference';
import type { Theme } from '@/domain/models/Theme';
import { applyTheme, initialTheme } from './theme';

interface ThemeState {
  theme: Theme;
  setTheme: (theme: Theme) => void;
}

const ThemeContext = createContext<ThemeState | null>(null);

/**
 * Thème de l'interface.
 * <p>
 * Le contexte ne porte que le choix ; les couleurs, elles, vivent entièrement
 * dans les jetons de design — un composant n'a donc jamais à lire le thème
 * pour s'afficher correctement, et ajouter un thème ne demandera pas d'y
 * revenir.
 */
export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<Theme>(initialTheme);

  const setTheme = useCallback((next: Theme) => {
    setThemeState(next);
    storeTheme(next);
  }, []);

  useEffect(() => applyTheme(theme), [theme]);

  const value = useMemo<ThemeState>(() => ({ theme, setTheme }), [theme, setTheme]);

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme(): ThemeState {
  const state = useContext(ThemeContext);
  if (!state) throw new Error('useTheme doit être utilisé sous <ThemeProvider>.');
  return state;
}
