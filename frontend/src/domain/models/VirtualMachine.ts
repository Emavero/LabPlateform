export type OperatingSystem = 'WINDOWS' | 'LINUX';
export type VmStatus = 'STOPPED' | 'RUNNING';
export type AccessProtocol = 'RDP' | 'SSH';

/** Informations d'accès : n'existent que pendant qu'une machine tourne. */
export interface ConnectionInfo {
  readonly host: string;
  readonly port: number;
  readonly protocol: AccessProtocol;
  readonly username: string;
  readonly password: string;
}

export interface VirtualMachine {
  readonly id: number;
  readonly os: OperatingSystem;
  readonly osName: string;
  readonly status: VmStatus;
  readonly startedAt: Date | null;
  readonly connection: ConnectionInfo | null;
}

export type VmAction = 'start' | 'stop';

export function isRunning(vm: VirtualMachine): boolean {
  return vm.status === 'RUNNING';
}

/** Action disponible selon l'état : une machine arrêtée se démarre, une machine démarrée s'arrête. */
export function availableAction(vm: VirtualMachine): VmAction {
  return isRunning(vm) ? 'stop' : 'start';
}

export const OS_FAMILY_LABELS: Record<OperatingSystem, string> = {
  WINDOWS: 'Windows',
  LINUX: 'Linux',
};
