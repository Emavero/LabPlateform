import { describe, expect, it } from 'vitest';
import {
  availableAction,
  isBusy,
  reachableAddress,
  type MachineStatus,
  type TargetMachine,
} from './TargetMachine';

const TRANSITIONAL: MachineStatus[] = ['PROVISIONING', 'STAGING', 'STOPPING'];

function machine(status: MachineStatus, internalIp: string | null = null): TargetMachine {
  return {
    status,
    statusName: status,
    transitioning: TRANSITIONAL.includes(status),
    internalIp,
  };
}

describe('cible partagée', () => {
  it('propose de démarrer une machine éteinte, et d’arrêter une machine en marche', () => {
    expect(availableAction(machine('TERMINATED'))).toBe('start');
    expect(availableAction(machine('RUNNING', '10.10.10.10'))).toBe('stop');
  });

  it('garde un libellé stable pendant une transition', () => {
    // Le bouton est inactif de toute façon ; un libellé qui change deux fois en
    // trois secondes se lit plus mal qu'un libellé stable.
    expect(availableAction(machine('STAGING'))).toBe('stop');
    expect(availableAction(machine('PROVISIONING'))).toBe('stop');
    expect(availableAction(machine('STOPPING'))).toBe('start');
  });

  it('désactive le bouton tant que la machine bouge', () => {
    for (const status of TRANSITIONAL) {
      expect(isBusy(machine(status))).toBe(true);
    }
    expect(isBusy(machine('RUNNING', '10.10.10.10'))).toBe(false);
    expect(isBusy(machine('TERMINATED'))).toBe(false);
  });

  it('ne montre l’adresse que si l’on peut s’y connecter', () => {
    expect(reachableAddress(machine('RUNNING', '10.10.10.10'))).toBe('10.10.10.10');
    expect(reachableAddress(machine('TERMINATED'))).toBeNull();
  });

  it('tait une adresse résiduelle annoncée hors marche', () => {
    // Le serveur ne devrait pas en envoyer, mais l'afficher ferait croire qu'on
    // peut déjà s'y connecter : on s'en tient à l'état.
    expect(reachableAddress(machine('STOPPING', '10.10.10.10'))).toBeNull();
  });
});
