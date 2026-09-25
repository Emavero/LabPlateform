import { describe, expect, it } from 'vitest';
import { normalizeFlag, validateFlag } from './flag';

const FLAG = '0123456789abcdef0123456789abcdef';

describe('validateFlag', () => {
  it('accepte un flag de 32 caractères hexadécimaux', () => {
    expect(validateFlag(FLAG)).toBeUndefined();
  });

  it('accepte un collage approximatif : espaces, majuscules, habillage', () => {
    expect(validateFlag(`  ${FLAG.toUpperCase()} `)).toBeUndefined();
    expect(validateFlag(`CYBM{${FLAG}}`)).toBeUndefined();
  });

  it('refuse une saisie vide, trop courte ou non hexadécimale', () => {
    expect(validateFlag('')).toBe('Le flag est obligatoire.');
    expect(validateFlag('   ')).toBe('Le flag est obligatoire.');
    expect(validateFlag(FLAG.slice(0, 31))).toContain('32 caractères');
    expect(validateFlag('z'.repeat(32))).toContain('32 caractères');
  });
});

describe('normalizeFlag', () => {
  it("retire l'habillage et la casse avant l'envoi au serveur", () => {
    expect(normalizeFlag(`  CYBM{${FLAG.toUpperCase()}}  `)).toBe(FLAG);
    expect(normalizeFlag(FLAG)).toBe(FLAG);
  });
});
