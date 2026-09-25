import type { VirtualMachine, VmAction } from '../models/VirtualMachine';
import type { LabRepository } from '../repositories/LabRepository';

export class ListVmsUseCase {
  constructor(private readonly lab: LabRepository) {}

  execute(): Promise<VirtualMachine[]> {
    return this.lab.list();
  }
}

export class GetVmUseCase {
  constructor(private readonly lab: LabRepository) {}

  execute(id: number): Promise<VirtualMachine> {
    return this.lab.get(id);
  }
}

export class StartVmUseCase {
  constructor(private readonly lab: LabRepository) {}

  execute(id: number): Promise<VirtualMachine> {
    return this.lab.start(id);
  }
}

export class StopVmUseCase {
  constructor(private readonly lab: LabRepository) {}

  execute(id: number): Promise<VirtualMachine> {
    return this.lab.stop(id);
  }
}

export class GetVmConsoleUseCase {
  constructor(private readonly lab: LabRepository) {}

  execute(id: number): Promise<string[]> {
    return this.lab.consoleLog(id);
  }
}

/** Démarre ou arrête selon l'action demandée : point d'entrée unique pour les boutons d'état. */
export class RunVmActionUseCase {
  constructor(
    private readonly startVm: StartVmUseCase,
    private readonly stopVm: StopVmUseCase,
  ) {}

  execute(id: number, action: VmAction): Promise<VirtualMachine> {
    return action === 'start' ? this.startVm.execute(id) : this.stopVm.execute(id);
  }
}
