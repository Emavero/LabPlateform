import { describe, expect, it } from 'vitest';
import { translateMessage } from './messages';
import { validateBoxDraft } from '@/domain/validation/boxDraft';
import {
  SECTION_MAX_MINUTES,
  SUMMARY_MAX_LENGTH,
  TITLE_MAX_LENGTH,
  validateDraft,
} from '@/domain/validation/courseDraft';
import { validateEmail, validateNewPassword } from '@/domain/validation/credentials';
import { validateFlag } from '@/domain/validation/flag';
import { EMPTY_BOX_DRAFT, EMPTY_DRAFT, type CourseDraft, type SectionDraft } from '@/domain/models/Admin';
import { AppError } from '@/domain/errors/AppError';
import {
  BODY_MAX_LENGTH,
  EMPTY_TICKET_DRAFT,
  SUBJECT_MAX_LENGTH,
  validateTicketDraft,
} from '@/domain/models/Support';

/**
 * Garde-fou contre la dérive : un message que le domaine sait produire et que
 * le catalogue ne connaît pas s'afficherait en français à un lecteur anglais.
 * Le test fait donc échouer les validations et vérifie que chaque phrase
 * obtenue a bien changé de langue.
 */
function assertTranslated(message: string): void {
  expect(translateMessage(message, 'en'), `message sans traduction anglaise : « ${message} »`).not.toBe(message);
}

describe('catalogue des messages', () => {
  it('laisse le français intact', () => {
    expect(translateMessage('Le nom est obligatoire.', 'fr')).toBe('Le nom est obligatoire.');
  });

  it('traduit les messages de saisie d’un compte', () => {
    assertTranslated(validateEmail('')!);
    assertTranslated(validateEmail('pas-une-adresse')!);
    const errors = validateNewPassword('court', '');
    assertTranslated(errors.password!);
    assertTranslated(errors.confirmPassword!);
    assertTranslated(validateNewPassword('a'.repeat(200), 'autre').password!);
    assertTranslated(validateNewPassword('assez-long-1', 'autre').confirmPassword!);
  });

  it('traduit les messages d’une machine', () => {
    const errors = validateBoxDraft({ ...EMPTY_BOX_DRAFT, name: '', ipAddress: '', userFlag: 'zz' });
    Object.values(errors).filter(Boolean).forEach(assertTranslated);
    assertTranslated(validateFlag('')!);
    assertTranslated(validateFlag('zz')!);
  });

  it('traduit les messages d’un cours', () => {
    const section = (changes: Partial<SectionDraft>): SectionDraft => ({
      id: null,
      title: 'Section',
      kind: 'THEORY',
      minutes: 10,
      content: 'Texte',
      videoUrl: '',
      questions: [],
      ...changes,
    });
    const drafts: CourseDraft[] = [
      { ...EMPTY_DRAFT, title: '' },
      { ...EMPTY_DRAFT, title: 'x'.repeat(TITLE_MAX_LENGTH + 1) },
      { ...EMPTY_DRAFT, summary: 'y'.repeat(SUMMARY_MAX_LENGTH + 1) },
      { ...EMPTY_DRAFT, sections: [] },
      { ...EMPTY_DRAFT, sections: [section({ title: '' })] },
      { ...EMPTY_DRAFT, sections: [section({ title: 'z'.repeat(TITLE_MAX_LENGTH + 1) })] },
      { ...EMPTY_DRAFT, sections: [section({ minutes: SECTION_MAX_MINUTES + 1 })] },
      { ...EMPTY_DRAFT, sections: [section({ videoUrl: 'pas-une-adresse' })] },
      { ...EMPTY_DRAFT, sections: [section({ content: '' })] },
      { ...EMPTY_DRAFT, sections: [section({ questions: [{ statement: '', choices: [] }] })] },
      { ...EMPTY_DRAFT, sections: [section({ questions: [{ statement: 'Q', choices: [] }] })] },
      {
        ...EMPTY_DRAFT,
        sections: [
          section({
            questions: [
              { statement: 'Q', choices: [{ label: '', correct: true }, { label: 'b', correct: false }] },
            ],
          }),
        ],
      },
      {
        ...EMPTY_DRAFT,
        sections: [
          section({
            questions: [
              { statement: 'Q', choices: [{ label: 'a', correct: false }, { label: 'b', correct: false }] },
            ],
          }),
        ],
      },
    ];

    const messages = drafts.flatMap((draft) => {
      const { title, summary, sections, bySection } = validateDraft(draft);
      return [title, summary, sections, ...Object.values(bySection)].filter(
        (value): value is string => typeof value === 'string',
      );
    });

    // Chaque brouillon doit vraiment avoir produit une erreur : sans cela le
    // test ne vérifierait rien.
    expect(messages.length).toBeGreaterThanOrEqual(drafts.length);
    messages.forEach(assertTranslated);
  });

  it('traduit les messages d’une demande d’assistance', () => {
    Object.values(validateTicketDraft(EMPTY_TICKET_DRAFT))
      .filter((value): value is string => typeof value === 'string')
      .forEach(assertTranslated);
    Object.values(
      validateTicketDraft({
        category: 'OTHER',
        subject: 's'.repeat(SUBJECT_MAX_LENGTH + 1),
        body: 'b'.repeat(BODY_MAX_LENGTH + 1),
      }),
    )
      .filter((value): value is string => typeof value === 'string')
      .forEach(assertTranslated);
  });

  it('traduit les erreurs transverses', () => {
    assertTranslated(AppError.validation({}).message);
    assertTranslated(new AppError('unexpected', 'Une erreur inattendue est survenue. Réessayez.').message);
  });

  /** Une phrase inconnue passe : mieux vaut du français qu'un champ vide. */
  it('rend tel quel un message hors catalogue', () => {
    expect(translateMessage('Phrase inédite.', 'en')).toBe('Phrase inédite.');
  });

  it('réinjecte les nombres des messages à limite', () => {
    expect(translateMessage('Au moins 8 caractères.', 'en')).toBe('At least 8 characters.');
    expect(translateMessage('Question 3 : cochez au moins une bonne réponse.', 'en')).toBe(
      'Question 3: tick at least one correct answer.',
    );
  });
});
