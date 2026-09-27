import { describe, expect, it } from 'vitest';
import {
  EMPTY_SCENARIO,
  EMPTY_STEP,
  MAX_STEPS,
  cleanDraft,
  hasBrokenStep,
  toDraft,
  validateScenarioDraft,
  type Scenario,
  type ScenarioDraft,
  type ScenarioStep,
} from './Scenario';

const draft = (changes: Partial<ScenarioDraft> = {}): ScenarioDraft => ({
  title: 'Intrusion guidée',
  brief: 'Mise en situation.',
  published: true,
  steps: [{ ...EMPTY_STEP, reference: 'sentinel' }],
  ...changes,
});

const step = (changes: Partial<ScenarioStep> = {}): ScenarioStep => ({
  id: 1,
  position: 1,
  kind: 'MACHINE',
  kindName: 'Machine à compromettre',
  reference: 'sentinel',
  name: 'Sentinel',
  instruction: null,
  objective: 'USER_FLAG',
  objectiveName: 'Flag utilisateur',
  missing: false,
  locked: false,
  done: false,
  ...changes,
});

const scenario = (steps: ScenarioStep[]): Scenario => ({
  slug: 'intrusion-guidee',
  title: 'Intrusion guidée',
  brief: 'Mise en situation.',
  published: true,
  updatedAt: new Date('2026-09-27T10:00:00Z'),
  steps,
  progress: { done: 0, total: steps.length, nextPosition: 1, complete: false, percent: 0 },
});

describe('saisie d’un scénario', () => {
  it('accepte un scénario renseigné', () => {
    expect(validateScenarioDraft(draft())).toEqual({});
  });

  it('refuse un titre et une mise en situation vides', () => {
    const errors = validateScenarioDraft(draft({ title: ' ', brief: '' }));

    expect(errors.title).toBeDefined();
    expect(errors.brief).toBeDefined();
  });

  /** Un scénario publié vide compterait pour terminé dès son ouverture. */
  it('refuse de publier un scénario sans étape', () => {
    expect(validateScenarioDraft(draft({ steps: [] })).steps).toBeDefined();
    expect(validateScenarioDraft(draft({ published: false, steps: [] })).steps).toBeUndefined();
  });

  it('signale l’étape dont la ressource n’est pas renseignée', () => {
    const errors = validateScenarioDraft(draft({ steps: [{ ...EMPTY_STEP, reference: '' }] }));

    expect(errors['step-0']).toBeDefined();
  });

  it('borne le nombre d’étapes', () => {
    const steps = Array.from({ length: MAX_STEPS + 1 }, (_, index) => ({
      ...EMPTY_STEP,
      reference: `machine-${index}`,
    }));

    expect(validateScenarioDraft(draft({ steps })).steps).toContain(String(MAX_STEPS));
  });
});

describe('préparation de l’envoi', () => {
  it('retire les étapes vides et rogne les espaces', () => {
    const cleaned = cleanDraft(
      draft({
        title: '  Titre  ',
        steps: [
          { ...EMPTY_STEP, reference: ' sentinel ', instruction: ' Consigne. ' },
          { ...EMPTY_STEP, reference: '   ' },
        ],
      }),
    );

    expect(cleaned.title).toBe('Titre');
    expect(cleaned.steps).toHaveLength(1);
    expect(cleaned.steps[0].reference).toBe('sentinel');
    expect(cleaned.steps[0].instruction).toBe('Consigne.');
  });

  it('relit un scénario publié comme brouillon d’édition', () => {
    const relu = toDraft(scenario([step({ instruction: 'Trouvez le service exposé.' })]));

    expect(relu.title).toBe('Intrusion guidée');
    expect(relu.published).toBe(true);
    expect(relu.steps[0]).toEqual({
      kind: 'MACHINE',
      reference: 'sentinel',
      instruction: 'Trouvez le service exposé.',
      objective: 'USER_FLAG',
    });
  });

  /** Une étape de cours n'a pas d'objectif : l'éditeur repart sur la valeur par défaut. */
  it('donne un objectif par défaut à une étape de cours relue', () => {
    const relu = toDraft(scenario([step({ kind: 'COURSE', objective: null, objectiveName: null })]));

    expect(relu.steps[0].objective).toBe('USER_FLAG');
  });

  it('part d’un brouillon avec une étape', () => {
    expect(EMPTY_SCENARIO.steps).toHaveLength(1);
    expect(EMPTY_SCENARIO.published).toBe(false);
  });
});

describe('lecture d’un scénario', () => {
  /** Un scénario qui bloque doit se voir : sinon l'équipe ne le corrige jamais. */
  it('signale un scénario dont une étape a perdu sa ressource', () => {
    expect(hasBrokenStep(scenario([step(), step({ id: 2, position: 2, missing: true })]))).toBe(true);
    expect(hasBrokenStep(scenario([step()]))).toBe(false);
  });
});
