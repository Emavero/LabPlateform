import { afterEach, describe, expect, it } from 'vitest';
import { readStoredTheme, storeTheme } from '@/data/storage/themePreference';
import { DEFAULT_THEME, isTheme, THEMES } from '@/domain/models/Theme';
import { initialTheme } from '@/presentation/theme/theme';

/** Stockage de navigateur réduit à ce que la préférence lui demande. */
function fakeStorage(initial: Record<string, string> = {}) {
  const values = new Map(Object.entries(initial));
  return {
    getItem: (key: string) => values.get(key) ?? null,
    setItem: (key: string, value: string) => void values.set(key, value),
  };
}

function withStorage(storage: unknown): void {
  (globalThis as { window?: unknown }).window = { localStorage: storage };
}

afterEach(() => {
  delete (globalThis as { window?: unknown }).window;
});

describe('thème', () => {
  it('ne reconnaît que les thèmes proposés', () => {
    expect(THEMES.every(isTheme)).toBe(true);
    expect(isTheme('solarized')).toBe(false);
    expect(isTheme(null)).toBe(false);
    expect(isTheme(undefined)).toBe(false);
  });

  it('retient le choix fait sur cet appareil', () => {
    withStorage(fakeStorage());
    storeTheme('light');
    expect(readStoredTheme()).toBe('light');
    expect(initialTheme()).toBe('light');
  });

  it('ignore une valeur stockée devenue invalide', () => {
    withStorage(fakeStorage({ 'cybermans.theme': 'sepia' }));
    expect(readStoredTheme()).toBeNull();
    expect(initialTheme()).toBe(DEFAULT_THEME);
  });

  it('retombe sur le thème sombre quand le stockage est refusé', () => {
    // Navigation privée : certains navigateurs lèvent au lieu de rendre vide.
    withStorage({
      getItem: () => {
        throw new Error('accès refusé');
      },
      setItem: () => {
        throw new Error('accès refusé');
      },
    });
    expect(() => storeTheme('light')).not.toThrow();
    expect(initialTheme()).toBe(DEFAULT_THEME);
  });
});
