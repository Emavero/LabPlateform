import type { JournalLine } from '../models/Journal';
import type { JournalRepository } from '../repositories/JournalRepository';

/** Au-delà, la page n'aide plus : on relit par famille, pas en défilant. */
const MAX_LINES = 200;

export class GetMyJournalUseCase {
  constructor(private readonly journal: JournalRepository) {}

  execute(limit = 50): Promise<JournalLine[]> {
    return this.journal.mine(Math.min(Math.max(limit, 1), MAX_LINES));
  }
}

export class GetPlatformJournalUseCase {
  constructor(private readonly journal: JournalRepository) {}

  execute(limit = 100): Promise<JournalLine[]> {
    return this.journal.platform(Math.min(Math.max(limit, 1), MAX_LINES));
  }
}
