/**
 * États d'une machine, repris du vocabulaire de Compute Engine. Miroir de
 * domain/lab/VmStatus côté serveur.
 */
export type MachineStatus = 'PROVISIONING' | 'STAGING' | 'RUNNING' | 'STOPPING' | 'TERMINATED';

/**
 * Cible partagée, telle que le serveur la rapporte.
 * <p>
 * `transitioning` vient du serveur et n'est pas recalculé ici : c'est le
 * domaine qui sait quels états sont de passage, et dupliquer cette liste dans
 * le client en ferait une deuxième vérité à tenir à jour.
 */
export interface TargetMachine {
  readonly status: MachineStatus;
  /** Libellé traduit par le serveur, selon la langue demandée. */
  readonly statusName: string;
  readonly transitioning: boolean;
  /** Adresse interne, présente seulement quand la machine tourne. */
  readonly internalIp: string | null;
}

export type MachineAction = 'start' | 'stop';

/**
 * Action que le bouton propose.
 * <p>
 * Une machine éteinte se démarre, une machine allumée s'arrête. Pendant une
 * transition, le bouton garde l'action qui mènera à l'état opposé de celui vers
 * lequel elle va : il est désactivé de toute façon, et un libellé qui change
 * deux fois en trois secondes se lit plus mal qu'un libellé stable.
 */
export function availableAction(machine: TargetMachine): MachineAction {
  switch (machine.status) {
    case 'RUNNING':
    case 'PROVISIONING':
    case 'STAGING':
      return 'stop';
    default:
      return 'start';
  }
}

/** Le bouton est inactif tant que la machine n'est pas posée sur un état stable. */
export function isBusy(machine: TargetMachine): boolean {
  return machine.transitioning;
}

/** L'adresse ne vaut d'être montrée que si l'on peut s'y connecter. */
export function reachableAddress(machine: TargetMachine): string | null {
  return machine.status === 'RUNNING' ? machine.internalIp : null;
}
