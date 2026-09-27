import type { AxiosInstance } from 'axios';
import type { Analytics } from '@/domain/models/Analytics';
import type { AnalyticsRepository } from '@/domain/repositories/AnalyticsRepository';
import { toAnalytics, type AnalyticsDto } from './mappers';

export class HttpAnalyticsRepository implements AnalyticsRepository {
  constructor(private readonly http: AxiosInstance) {}

  async get(windowDays: number): Promise<Analytics> {
    const { data } = await this.http.get<AnalyticsDto>('/admin/analytics', { params: { windowDays } });
    return toAnalytics(data);
  }
}
