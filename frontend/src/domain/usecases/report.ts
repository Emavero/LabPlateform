import type { ActivityReport } from '../models/Report';
import type { ReportRepository } from '../repositories/ReportRepository';

export class GetActivityReportUseCase {
  constructor(private readonly reports: ReportRepository) {}

  execute(days: number): Promise<ActivityReport> {
    return this.reports.activity(days);
  }
}
