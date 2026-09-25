import type { VirtualMachine } from '../models/VirtualMachine';

export interface LabRepository {
  list(): Promise<VirtualMachine[]>;
  get(id: number): Promise<VirtualMachine>;
  start(id: number): Promise<VirtualMachine>;
  stop(id: number): Promise<VirtualMachine>;
  consoleLog(id: number): Promise<string[]>;
}
