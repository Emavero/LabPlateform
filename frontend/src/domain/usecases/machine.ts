import type { MachineAction, TargetMachine } from '../models/TargetMachine';
import type { MachineRepository } from '../repositories/MachineRepository';

export class GetMachineStatusUseCase {
  constructor(private readonly machine: MachineRepository) {}

  execute(): Promise<TargetMachine> {
    return this.machine.status();
  }
}

/**
 * Allume ou éteint la cible, selon l'action demandée.
 * <p>
 * Un seul cas d'usage pour les deux, comme pour les machines d'attaque : le
 * bouton est unique, et séparer ferait écrire la même condition des deux côtés.
 */
export class RunMachineActionUseCase {
  constructor(private readonly machine: MachineRepository) {}

  execute(action: MachineAction): Promise<TargetMachine> {
    return action === 'start' ? this.machine.start() : this.machine.stop();
  }
}
