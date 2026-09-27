import type { Scenario, ScenarioDraft } from '../models/Scenario';

export interface ScenarioRepository {
  /** Scénarios publiés, avec l'avancement du compte connecté. */
  listPublished(): Promise<Scenario[]>;
  get(slug: string): Promise<Scenario>;
  /** Tous les scénarios, brouillons compris. Refusé par le serveur aux non-administrateurs. */
  listAll(): Promise<Scenario[]>;
  create(draft: ScenarioDraft): Promise<string>;
  update(slug: string, draft: ScenarioDraft): Promise<string>;
  remove(slug: string): Promise<void>;
}
