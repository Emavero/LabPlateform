import { describe, expect, it } from 'vitest';
import { barWidth, compact, deltaPercent } from './Analytics';

describe('Analytics', () => {
  it('compare une période à la précédente', () => {
    expect(deltaPercent(120, 100)).toBe(20);
    expect(deltaPercent(80, 100)).toBe(-20);
    expect(deltaPercent(0, 0)).toBe(0);
  });

  /** Partir de zéro n'est pas une hausse infinie : l'interface dira « nouveau ». */
  it('ne rend aucune variation quand la période précédente était vide', () => {
    expect(deltaPercent(42, 0)).toBeNull();
  });

  /** Le séparateur décimal suit la locale : « 1,3 k » en français. */
  it('abrège les grands nombres selon la locale', () => {
    expect(compact(999, 'fr-FR')).toBe('999');
    expect(compact(1284, 'fr-FR')).toBe('1,3 k');
    expect(compact(2_400_000, 'fr-FR')).toBe('2,4 M');
    expect(compact(1284, 'en-US')).toBe('1.3 k');
  });

  /**
   * Les barres sont proportionnées à la plus grande valeur, pas au total : c'est
   * ce qui garde les écarts lisibles quand une entrée écrase les autres.
   */
  it('proportionne les barres à la plus grande valeur du classement', () => {
    const entries = [{ count: 40 }, { count: 20 }, { count: 10 }];

    expect(barWidth(40, entries)).toBe(100);
    expect(barWidth(20, entries)).toBe(50);
    expect(barWidth(10, entries)).toBe(25);
  });

  it('ne divise pas par zéro sur un classement vide', () => {
    expect(barWidth(0, [])).toBe(0);
    expect(barWidth(5, [{ count: 0 }])).toBe(0);
  });
});
