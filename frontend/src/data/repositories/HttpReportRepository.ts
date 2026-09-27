import type { AxiosInstance } from 'axios';
import type { ActivityReport } from '@/domain/models/Report';
import type { ReportRepository } from '@/domain/repositories/ReportRepository';

type Iso<T, K extends keyof T> = Omit<T, K> & Record<Extract<K, string>, string>;
type ReportDto = Omit<Iso<ActivityReport, 'from' | 'to'>, 'machines' | 'courses'> & {
  machines: Iso<ActivityReport['machines'][number], 'lastAt'>[];
  courses: Iso<ActivityReport['courses'][number], 'lastAt'>[];
};

export class HttpReportRepository implements ReportRepository {
  constructor(private readonly http: AxiosInstance) {}

  async activity(days: number): Promise<ActivityReport> {
    const { data } = await this.http.get<ReportDto>('/reports/activity', { params: { days } });
    return {
      ...data,
      from: new Date(data.from),
      to: new Date(data.to),
      machines: data.machines.map((line) => ({ ...line, lastAt: new Date(line.lastAt) })),
      courses: data.courses.map((line) => ({ ...line, lastAt: new Date(line.lastAt) })),
    };
  }
}
