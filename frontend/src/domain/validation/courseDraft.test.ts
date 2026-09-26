import { describe, expect, it } from 'vitest';
import { EMPTY_SECTION, type CourseDraft } from '../models/Admin';
import { draftHasErrors, validateDraft } from './courseDraft';

const draft: CourseDraft = {
  title: 'Analyser un journal',
  track: 'FORENSICS',
  level: 'EASY',
  summary: 'Résumé.',
  sections: [{ ...EMPTY_SECTION, title: 'Introduction', content: 'Du texte.' }],
};

describe('validateDraft', () => {
  it('accepte un brouillon complet', () => {
    expect(draftHasErrors(validateDraft(draft))).toBe(false);
  });

  it('exige un titre de cours et un titre par section', () => {
    expect(validateDraft({ ...draft, title: '  ' }).title).toBe('Le titre est obligatoire.');
    const sections = [{ ...EMPTY_SECTION, title: '', content: 'Texte.' }];
    expect(validateDraft({ ...draft, sections }).bySection[0]).toContain('titre de la section');
  });

  it('exige au moins un texte ou une vidéo par section', () => {
    const sections = [{ ...EMPTY_SECTION, title: 'Vide' }];
    expect(validateDraft({ ...draft, sections }).bySection[0]).toContain('au moins un texte ou une vidéo');
  });

  it('accepte une section qui ne porte qu’une vidéo', () => {
    const sections = [{ ...EMPTY_SECTION, title: 'Démo', videoUrl: 'https://vimeo.com/123456789' }];
    expect(draftHasErrors(validateDraft({ ...draft, sections }))).toBe(false);
  });

  it('refuse une adresse de vidéo qui ne soit pas http(s)', () => {
    const sections = [{ ...EMPTY_SECTION, title: 'Piégée', content: 'x', videoUrl: 'javascript:alert(1)' }];
    expect(validateDraft({ ...draft, sections }).bySection[0]).toContain('https://');
  });

  it('refuse une durée hors bornes et un cours sans section', () => {
    const sections = [{ ...EMPTY_SECTION, title: 'Longue', content: 'x', minutes: 9999 }];
    expect(validateDraft({ ...draft, sections }).bySection[0]).toContain('0 à 600');
    expect(validateDraft({ ...draft, sections: [] }).sections).toContain('au moins une section');
  });
});
