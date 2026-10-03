import type { TargetMachine } from '../models/TargetMachine';

export interface MachineRepository {
  start(): Promise<TargetMachine>;
  stop(): Promise<TargetMachine>;
  status(): Promise<TargetMachine>;
}
