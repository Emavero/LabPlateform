import type { Writeup, WriteupDraft } from '../models/Writeup';

export interface WriteupRepository {
  list(slug: string): Promise<Writeup[]>;
  save(slug: string, draft: WriteupDraft): Promise<Writeup>;
  remove(slug: string): Promise<void>;
}
