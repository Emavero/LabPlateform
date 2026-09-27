import type { AxiosInstance } from 'axios';
import type { LabExposure } from '@/domain/models/Exposure';
import type { ExposureRepository } from '@/domain/repositories/ExposureRepository';

/** Aucune date dans cette réponse : elle passe telle quelle. */
export class HttpExposureRepository implements ExposureRepository {
  constructor(private readonly http: AxiosInstance) {}

  async get(): Promise<LabExposure> {
    const { data } = await this.http.get<LabExposure>('/exposure');
    return data;
  }
}
