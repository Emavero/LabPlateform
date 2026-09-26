import type { AxiosInstance } from 'axios';
import type { JournalLine } from '@/domain/models/Journal';
import type { JournalRepository } from '@/domain/repositories/JournalRepository';
import { toJournalLine, type JournalLineDto } from './mappers';

export class HttpJournalRepository implements JournalRepository {
  constructor(private readonly http: AxiosInstance) {}

  async mine(limit: number): Promise<JournalLine[]> {
    const { data } = await this.http.get<JournalLineDto[]>('/journal', { params: { limit } });
    return data.map(toJournalLine);
  }

  async platform(limit: number): Promise<JournalLine[]> {
    const { data } = await this.http.get<JournalLineDto[]>('/admin/journal', { params: { limit } });
    return data.map(toJournalLine);
  }
}
