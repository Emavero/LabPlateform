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

  it('exige au moins un texte, une vidéo ou un quiz par section', () => {
    const sections = [{ ...EMPTY_SECTION, title: 'Vide' }];
    expect(validateDraft({ ...draft, sections }).bySection[0]).toContain('au moins un texte, une vidéo ou un quiz');
  });

  it("accepte une section qui n'est qu'un quiz", () => {
    const sections = [
      {
        ...EMPTY_SECTION,
        title: 'Quiz',
        kind: 'QUIZ' as const,
        questions: [
          {
            statement: 'Que faire en premier ?',
            choices: [
              { label: 'Capturer la mémoire', correct: true },
              { label: 'Éteindre', correct: false },
            ],
          },
        ],
      },
    ];
    expect(draftHasErrors(validateDraft({ ...draft, sections }))).toBe(false);
  });

  it('refuse une question sans énoncé, sans bonne réponse ou à proposition vide', () => {
    const withQuestion = (question: {
      statement: string;
      choices: { label: string; correct: boolean }[];
    }) => ({ ...draft, sections: [{ ...EMPTY_SECTION, title: 'Quiz', questions: [question] }] });

    expect(
      validateDraft(withQuestion({ statement: '  ', choices: [{ label: 'A', correct: true }, { label: 'B', correct: false }] }))
        .bySection[0],
    ).toContain("l'énoncé est obligatoire");
    expect(
      validateDraft(withQuestion({ statement: 'Q', choices: [{ label: 'A', correct: false }, { label: 'B', correct: false }] }))
        .bySection[0],
    ).toContain('au moins une bonne réponse');
    expect(
      validateDraft(withQuestion({ statement: 'Q', choices: [{ label: '  ', correct: true }, { label: 'B', correct: false }] }))
        .bySection[0],
    ).toContain('ne peut pas être vide');
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
