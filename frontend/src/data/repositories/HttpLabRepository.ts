import type { AxiosInstance } from 'axios';
import type { VirtualMachine } from '@/domain/models/VirtualMachine';
import type { LabRepository } from '@/domain/repositories/LabRepository';
import { toVirtualMachine, type VmDto } from './mappers';

export class HttpLabRepository implements LabRepository {
  constructor(private readonly http: AxiosInstance) {}

  async list(): Promise<VirtualMachine[]> {
    const { data } = await this.http.get<VmDto[]>('/labs/vms');
    return data.map(toVirtualMachine);
  }

  async get(id: number): Promise<VirtualMachine> {
    const { data } = await this.http.get<VmDto>(`/labs/vms/${id}`);
    return toVirtualMachine(data);
  }

  async start(id: number): Promise<VirtualMachine> {
    const { data } = await this.http.post<VmDto>(`/labs/vms/${id}/start`);
    return toVirtualMachine(data);
  }

  async stop(id: number): Promise<VirtualMachine> {
    const { data } = await this.http.post<VmDto>(`/labs/vms/${id}/stop`);
    return toVirtualMachine(data);
  }

  async consoleLog(id: number): Promise<string[]> {
    const { data } = await this.http.get<string[]>(`/labs/vms/${id}/logs`);
    return data;
  }
}
