import type { AxiosInstance } from 'axios';
import type { Scenario, ScenarioDraft } from '@/domain/models/Scenario';
import type { ScenarioRepository } from '@/domain/repositories/ScenarioRepository';

type ScenarioDto = Omit<Scenario, 'updatedAt'> & { updatedAt: string };

export class HttpScenarioRepository implements ScenarioRepository {
  constructor(private readonly http: AxiosInstance) {}

  async listPublished(): Promise<Scenario[]> {
    const { data } = await this.http.get<ScenarioDto[]>('/scenarios');
    return data.map(toScenario);
  }

  async get(slug: string): Promise<Scenario> {
    const { data } = await this.http.get<ScenarioDto>(`/scenarios/${encodeURIComponent(slug)}`);
    return toScenario(data);
  }

  async listAll(): Promise<Scenario[]> {
    const { data } = await this.http.get<ScenarioDto[]>('/admin/scenarios');
    return data.map(toScenario);
  }

  async create(draft: ScenarioDraft): Promise<string> {
    const { data } = await this.http.post<{ slug: string }>('/admin/scenarios', draft);
    return data.slug;
  }

  async update(slug: string, draft: ScenarioDraft): Promise<string> {
    const { data } = await this.http.put<{ slug: string }>(`/admin/scenarios/${encodeURIComponent(slug)}`, draft);
    return data.slug;
  }

  async remove(slug: string): Promise<void> {
    await this.http.delete(`/admin/scenarios/${encodeURIComponent(slug)}`);
  }
}

function toScenario(dto: ScenarioDto): Scenario {
  return { ...dto, updatedAt: new Date(dto.updatedAt) };
}
