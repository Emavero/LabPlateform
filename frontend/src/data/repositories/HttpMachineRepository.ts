import type { AxiosInstance } from 'axios';
import type { TargetMachine } from '@/domain/models/TargetMachine';
import type { MachineRepository } from '@/domain/repositories/MachineRepository';
import { toTargetMachine, type TargetMachineDto } from './mappers';

export class HttpMachineRepository implements MachineRepository {
  constructor(private readonly http: AxiosInstance) {}

  async start(): Promise<TargetMachine> {
    const { data } = await this.http.post<TargetMachineDto>('/machine/start');
    return toTargetMachine(data);
  }

  async stop(): Promise<TargetMachine> {
    const { data } = await this.http.post<TargetMachineDto>('/machine/stop');
    return toTargetMachine(data);
  }

  async status(): Promise<TargetMachine> {
    const { data } = await this.http.get<TargetMachineDto>('/machine/status');
    return toTargetMachine(data);
  }
}
