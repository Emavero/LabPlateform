import { describe, expect, it } from 'vitest';
import { EMPTY_CASE, EMPTY_SECTION, EMPTY_STAGE, type CourseDraft } from '../models/Admin';
import { draftHasErrors, validateDraft } from './courseDraft';

const draft: CourseDraft = {
  title: 'Analyser un journal',
  topic: 'LOG_ANALYSIS',
  level: 'EASY',
  summary: 'Résumé.',
  sections: [{ ...EMPTY_SECTION, title: 'Introduction', content: 'Du texte.' }],
  attackSummary: '',
  stages: [],
  realCase: EMPTY_CASE,
  designers: [],
};

const stage = { ...EMPTY_STAGE, name: 'Hameçonnage', description: 'Une pièce jointe.' };

const realCase = {
  sector: 'Cabinet comptable',
  situation: 'Un poste lent un vendredi.',
  stake: '300 dossiers clients.',
  outcome: 'Onze notifications au lieu de 300.',
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

describe('dossier du cours', () => {
  it('accepte un cours sans chemin d’attaque, sans cas d’usage et sans concepteur', () => {
    // C'est le cas de tous les cours écrits avant ces sections.
    expect(draftHasErrors(validateDraft(draft))).toBe(false);
  });

  it('accepte un chemin d’attaque complet', () => {
    const complete = { ...draft, attackSummary: 'De la pièce jointe à l’exfiltration.', stages: [stage] };

    expect(validateDraft(complete).attackPath).toBeUndefined();
    expect(draftHasErrors(validateDraft(complete))).toBe(false);
  });

  it('refuse un résumé de chaîne sans étape', () => {
    const errors = validateDraft({ ...draft, attackSummary: 'Une chaîne annoncée.' });

    expect(errors.attackPath).toContain('au moins une étape');
    expect(draftHasErrors(errors)).toBe(true);
  });

  it('refuse des étapes sans résumé de chaîne', () => {
    expect(validateDraft({ ...draft, stages: [stage] }).attackPath).toContain('ce que ces étapes enchaînent');
  });

  it('ignore une étape laissée entièrement vide', () => {
    // Un bouton « Ajouter » cliqué puis abandonné n'est pas une erreur.
    const withBlank = { ...draft, attackSummary: 'Une chaîne.', stages: [stage, EMPTY_STAGE] };

    expect(validateDraft(withBlank).attackPath).toBeUndefined();
  });

  it('refuse une étape à moitié remplie', () => {
    const half = { ...draft, attackSummary: 'Une chaîne.', stages: [{ ...EMPTY_STAGE, name: 'Accès' }] };

    expect(validateDraft(half).attackPath).toContain('description est obligatoire');
  });

  it('refuse un cas d’usage partiellement rempli', () => {
    const partial = { ...draft, realCase: { ...EMPTY_CASE, sector: 'Banque', situation: 'Une alerte.' } };

    expect(validateDraft(partial).realCase).toContain('quatre champs');
    expect(validateDraft({ ...draft, realCase }).realCase).toBeUndefined();
  });

  it('exige le nom et le rôle d’un concepteur renseigné', () => {
    const noRole = { ...draft, designers: [{ name: 'Awa Diallo', role: '', avatarUrl: '' }] };
    const noName = { ...draft, designers: [{ name: '', role: 'Analyste', avatarUrl: '' }] };

    expect(validateDraft(noRole).designers).toContain('rôle est obligatoire');
    expect(validateDraft(noName).designers).toContain('nom est obligatoire');
  });

  it('ignore un concepteur laissé entièrement vide, et accepte un avatar absent', () => {
    // Une ligne de concepteur ouverte puis abandonnée n'est pas une erreur.
    const designers = [
      { name: 'Awa Diallo', role: 'Analyste', avatarUrl: '' },
      { name: '', role: '', avatarUrl: '' },
    ];

    expect(validateDraft({ ...draft, designers }).designers).toBeUndefined();
  });
});
