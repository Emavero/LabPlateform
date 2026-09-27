import type { Language } from '@/domain/models/Language';

/**
 * Catalogue de traduction : des clés plates, pointées par domaine
 * (`billing.title`), vers le texte affiché.
 * <p>
 * Plat, et non imbriqué, pour une raison précise : TypeScript peut alors
 * exiger que le catalogue anglais porte exactement les mêmes clés que le
 * français, et une traduction oubliée devient une erreur de compilation plutôt
 * qu'un trou découvert par un utilisateur.
 */
export type Catalogue = Record<string, string>;

/** Valeurs injectées dans un texte à trous : `{count}`, `{name}`. */
export type Vars = Record<string, string | number>;

export type { Language };

/**
 * Remplace les trous d'un texte. Un trou sans valeur est laissé tel quel :
 * mieux vaut voir `{count}` à l'écran et corriger, qu'une phrase amputée.
 */
export function interpolate(text: string, vars?: Vars): string {
  if (!vars) return text;
  return text.replace(/\{(\w+)\}/g, (whole, key: string) => (key in vars ? String(vars[key]) : whole));
}
