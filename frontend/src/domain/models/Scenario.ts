/**
 * Scénarios d'exercice, vus du navigateur.
 * <p>
 * L'avancement arrive calculé : il se déduit des flags validés et des sections
 * terminées, et le recalculer ici donnerait deux façons de compter qui
 * finiraient par ne plus dire la même chose.
 */
export type ScenarioStepKind = 'MACHINE' | 'COURSE';

export type ScenarioObjective = 'USER_FLAG' | 'ROOT_FLAG' | 'BOTH_FLAGS';

export const STEP_KINDS: readonly ScenarioStepKind[] = ['MACHINE', 'COURSE'];

export const STEP_OBJECTIVES: readonly ScenarioObjective[] = ['USER_FLAG', 'ROOT_FLAG', 'BOTH_FLAGS'];

export interface ScenarioStep {
  readonly id: number | null;
  readonly position: number;
  readonly kind: ScenarioStepKind;
  readonly kindName: string;
  /** Lien de la machine ou du cours visé. */
  readonly reference: string;
  /** Nom affichable, ou le lien si la ressource a disparu du catalogue. */
  readonly name: string;
  readonly instruction: string | null;
  readonly objective: ScenarioObjective | null;
  readonly objectiveName: string | null;
  /** La ressource n'existe plus : l'étape est infranchissable. */
  readonly missing: boolean;
  /** Machine réservée aux abonnés : l'étape se voit, son détail non. */
  readonly locked: boolean;
  readonly done: boolean;
}

export interface ScenarioProgress {
  readonly done: number;
  readonly total: number;
  readonly nextPosition: number | null;
  readonly complete: boolean;
  readonly percent: number;
}

export interface Scenario {
  readonly slug: string;
  readonly title: string;
  readonly brief: string;
  readonly published: boolean;
  readonly updatedAt: Date;
  readonly steps: readonly ScenarioStep[];
  readonly progress: ScenarioProgress;
}

/** Étape en cours de saisie dans l'éditeur. */
export interface StepDraft {
  readonly kind: ScenarioStepKind;
  readonly reference: string;
  readonly instruction: string;
  readonly objective: ScenarioObjective;
}

export interface ScenarioDraft {
  readonly title: string;
  readonly brief: string;
  readonly published: boolean;
  readonly steps: readonly StepDraft[];
}

export const EMPTY_STEP: StepDraft = { kind: 'MACHINE', reference: '', instruction: '', objective: 'USER_FLAG' };

export const EMPTY_SCENARIO: ScenarioDraft = { title: '', brief: '', published: false, steps: [EMPTY_STEP] };

export const MAX_STEPS = 20;

/**
 * Règles de saisie, miroir de celles du serveur qui reste l'autorité.
 * <p>
 * Publier sans étape est refusé ici aussi : un scénario publié vide compterait
 * pour terminé dès son ouverture.
 */
export function validateScenarioDraft(draft: ScenarioDraft): Partial<Record<string, string>> {
  const errors: Record<string, string> = {};
  if (!draft.title.trim()) {
    errors.title = 'Le titre est obligatoire.';
  }
  if (!draft.brief.trim()) {
    errors.brief = 'La mise en situation est obligatoire.';
  }
  const steps = draft.steps.filter((step) => step.reference.trim());
  if (draft.published && steps.length === 0) {
    errors.steps = 'Un scénario publié comporte au moins une étape.';
  }
  if (draft.steps.length > MAX_STEPS) {
    errors.steps = `Un scénario comporte au plus ${MAX_STEPS} étapes.`;
  }
  draft.steps.forEach((step, index) => {
    if (!step.reference.trim()) {
      errors[`step-${index}`] = 'Chaque étape désigne une machine ou un cours.';
    }
  });
  return errors;
}

/** Brouillon envoyé au serveur : les étapes vides sont retirées, jamais envoyées. */
export function cleanDraft(draft: ScenarioDraft): ScenarioDraft {
  return {
    ...draft,
    title: draft.title.trim(),
    brief: draft.brief.trim(),
    steps: draft.steps
      .filter((step) => step.reference.trim())
      .map((step) => ({ ...step, reference: step.reference.trim(), instruction: step.instruction.trim() })),
  };
}

/** Scénario relu en brouillon : l'éditeur repart de ce qui est publié. */
export function toDraft(scenario: Scenario): ScenarioDraft {
  return {
    title: scenario.title,
    brief: scenario.brief,
    published: scenario.published,
    steps: scenario.steps.map((step) => ({
      kind: step.kind,
      reference: step.reference,
      instruction: step.instruction ?? '',
      objective: step.objective ?? 'USER_FLAG',
    })),
  };
}

/** Une étape infranchissable bloque le scénario : l'administration doit le voir. */
export function hasBrokenStep(scenario: Scenario): boolean {
  return scenario.steps.some((step) => step.missing);
}
