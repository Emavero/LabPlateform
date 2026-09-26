import { describe, expect, it } from 'vitest';
import { myWriteup, validateWriteup, type Writeup } from './Writeup';

const writeup = (handle: string, mine: boolean): Writeup => ({
  handle,
  mine,
  title: 'Titre',
  content: 'Contenu',
  published: true,
  createdAt: new Date('2026-09-01T00:00:00Z'),
  updatedAt: new Date('2026-09-02T00:00:00Z'),
});

describe('validateWriteup', () => {
  it('accepte un compte rendu complet', () => {
    const errors = validateWriteup({ title: 'Ma méthode', content: 'Injection puis cron.', published: true });
    expect(errors.title).toBeUndefined();
    expect(errors.content).toBeUndefined();
  });

  it('exige un titre et un contenu', () => {
    const errors = validateWriteup({ title: '  ', content: '   ', published: false });
    expect(errors.title).toBe('Le titre est obligatoire.');
    expect(errors.content).toBe('Le compte rendu est vide.');
  });

  it('borne la longueur du titre', () => {
    const errors = validateWriteup({ title: 'x'.repeat(200), content: 'Contenu.', published: false });
    expect(errors.title).toContain('128');
  });
});

describe('myWriteup', () => {
  it("repère celui du lecteur parmi ceux des autres", () => {
    expect(myWriteup([writeup('bob', false), writeup('moi', true)])?.handle).toBe('moi');
    expect(myWriteup([writeup('bob', false)])).toBeUndefined();
  });
});
