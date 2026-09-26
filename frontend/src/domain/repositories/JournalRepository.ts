import type { JournalLine } from '../models/Journal';

export interface JournalRepository {
  /** Journal du compte connecté. */
  mine(limit: number): Promise<JournalLine[]>;
  /** Journal de toute la plateforme. Le serveur le refuse à un non-administrateur. */
  platform(limit: number): Promise<JournalLine[]>;
}
