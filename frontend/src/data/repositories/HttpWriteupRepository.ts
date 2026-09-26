import type { AxiosInstance } from 'axios';
import type { Writeup, WriteupDraft } from '@/domain/models/Writeup';
import type { WriteupRepository } from '@/domain/repositories/WriteupRepository';

type WriteupDto = Omit<Writeup, 'createdAt' | 'updatedAt'> & { createdAt: string; updatedAt: string };

export class HttpWriteupRepository implements WriteupRepository {
  constructor(private readonly http: AxiosInstance) {}

  async list(slug: string): Promise<Writeup[]> {
    const { data } = await this.http.get<WriteupDto[]>(this.path(slug));
    return data.map(toWriteup);
  }

  async save(slug: string, draft: WriteupDraft): Promise<Writeup> {
    const { data } = await this.http.put<WriteupDto>(`${this.path(slug)}/mine`, draft);
    return toWriteup(data);
  }

  async remove(slug: string): Promise<void> {
    await this.http.delete(`${this.path(slug)}/mine`);
  }

  private path(slug: string): string {
    return `/boxes/${encodeURIComponent(slug)}/writeups`;
  }
}

function toWriteup(dto: WriteupDto): Writeup {
  return { ...dto, createdAt: new Date(dto.createdAt), updatedAt: new Date(dto.updatedAt) };
}
