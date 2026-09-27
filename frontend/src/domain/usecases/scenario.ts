import { AppError } from '../errors/AppError';
import { cleanDraft, validateScenarioDraft, type Scenario, type ScenarioDraft } from '../models/Scenario';
import type { ScenarioRepository } from '../repositories/ScenarioRepository';

export class ListScenariosUseCase {
  constructor(private readonly scenarios: ScenarioRepository) {}

  execute(): Promise<Scenario[]> {
    return this.scenarios.listPublished();
  }
}

export class GetScenarioUseCase {
  constructor(private readonly scenarios: ScenarioRepository) {}

  execute(slug: string): Promise<Scenario> {
    return this.scenarios.get(slug);
  }
}

export class ListAllScenariosUseCase {
  constructor(private readonly scenarios: ScenarioRepository) {}

  execute(): Promise<Scenario[]> {
    return this.scenarios.listAll();
  }
}

/** Enregistre un scénario, après vérification de la saisie. */
export class SaveScenarioUseCase {
  constructor(private readonly scenarios: ScenarioRepository) {}

  execute(draft: ScenarioDraft, slug?: string): Promise<string> {
    const errors = validateScenarioDraft(draft);
    if (Object.keys(errors).length > 0) return Promise.reject(AppError.validation(errors as Record<string, string>));
    const cleaned = cleanDraft(draft);
    return slug ? this.scenarios.update(slug, cleaned) : this.scenarios.create(cleaned);
  }
}

export class DeleteScenarioUseCase {
  constructor(private readonly scenarios: ScenarioRepository) {}

  execute(slug: string): Promise<void> {
    return this.scenarios.remove(slug);
  }
}
