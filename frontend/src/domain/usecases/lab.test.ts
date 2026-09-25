import { describe, expect, it } from 'vitest';
import type { VirtualMachine } from '../models/VirtualMachine';
import { availableAction } from '../models/VirtualMachine';
import type { LabRepository } from '../repositories/LabRepository';
import { RunVmActionUseCase, StartVmUseCase, StopVmUseCase } from './lab';

const stopped: VirtualMachine = { id: 1, os: 'LINUX', osName: 'Ubuntu', status: 'STOPPED', startedAt: null, connection: null };
const running: VirtualMachine = {
  ...stopped,
  status: 'RUNNING',
  startedAt: new Date(),
  connection: { host: '10.42.0.2', port: 22, protocol: 'SSH', username: 'labuser', password: 'secret' },
};

function repository(calls: string[]): LabRepository {
  return {
    list: async () => [stopped],
    get: async () => stopped,
    start: async (id) => (calls.push(`start:${id}`), running),
    stop: async (id) => (calls.push(`stop:${id}`), stopped),
    consoleLog: async () => [],
  };
}

describe('RunVmActionUseCase', () => {
  it("choisit l'action selon l'état de la machine", async () => {
    const calls: string[] = [];
    const repo = repository(calls);
    const useCase = new RunVmActionUseCase(new StartVmUseCase(repo), new StopVmUseCase(repo));

    const started = await useCase.execute(stopped.id, availableAction(stopped));
    await useCase.execute(started.id, availableAction(started));

    expect(calls).toEqual(['start:1', 'stop:1']);
  });
});
