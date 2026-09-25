import type { AxiosInstance } from 'axios';
import type { Box, FlagKind } from '@/domain/models/Box';
import type { BoxRepository, FlagSubmission } from '@/domain/repositories/BoxRepository';
import { toBox, toProgress, type BoxDto, type FlagSubmissionDto } from './mappers';

export class HttpBoxRepository implements BoxRepository {
  constructor(private readonly http: AxiosInstance) {}

  async list(): Promise<Box[]> {
    const { data } = await this.http.get<BoxDto[]>('/boxes');
    return data.map(toBox);
  }

  async get(slug: string): Promise<Box> {
    const { data } = await this.http.get<BoxDto>(`/boxes/${encodeURIComponent(slug)}`);
    return toBox(data);
  }

  async submitFlag(slug: string, kind: FlagKind, flag: string): Promise<FlagSubmission> {
    const { data } = await this.http.post<FlagSubmissionDto>(`/boxes/${encodeURIComponent(slug)}/flags`, {
      kind,
      flag,
    });
    return { ...data, progress: toProgress(data.progress) };
  }
}
