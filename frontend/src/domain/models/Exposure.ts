/**
 * Surface d'attaque du lab, vue du navigateur.
 * <p>
 * Tout est calculé par le serveur : la notation est une règle du domaine, elle
 * ne doit pas exister en deux versions qui finiraient par se contredire. Le
 * navigateur n'ajoute ici que la mise en forme.
 */
export type ExposureLevel = 'CRITICAL' | 'HIGH' | 'MODERATE' | 'LOW';

export type AttackLink = 'ENTRY' | 'SAME_SEGMENT' | 'SAME_SYSTEM';

export interface ExposedService {
  readonly code: 'SSH' | 'RDP';
  readonly label: string;
  readonly port: number;
  readonly description: string;
}

export interface ExposureSignal {
  readonly code: string;
  readonly label: string;
}

export interface TargetExposure {
  readonly slug: string;
  readonly name: string;
  readonly service: ExposedService;
  readonly segment: string;
  /** Absente quand la cible est réservée et le compte non abonné. */
  readonly address: string | null;
  readonly score: number;
  readonly level: ExposureLevel;
  readonly levelName: string;
  readonly signals: readonly ExposureSignal[];
  readonly advice: string;
  readonly locked: boolean;
}

export interface AttackHop {
  readonly slug: string;
  readonly name: string;
  readonly service: ExposedService;
  readonly segment: string;
  readonly effort: number;
  readonly link: AttackLink;
  readonly linkName: string;
}

export interface AttackPath {
  readonly objectiveSlug: string;
  readonly objectiveName: string;
  readonly effort: number;
  readonly hops: readonly AttackHop[];
}

export interface SegmentTally {
  readonly segment: string;
  readonly targets: number;
}

export interface LabExposure {
  readonly targets: readonly TargetExposure[];
  readonly paths: readonly AttackPath[];
  readonly segments: readonly SegmentTally[];
  readonly unlocked: boolean;
  readonly lockedOut: number;
}

/** Paliers dans l'ordre de gravité : sert à grouper et à filtrer. */
export const EXPOSURE_LEVELS: readonly ExposureLevel[] = ['CRITICAL', 'HIGH', 'MODERATE', 'LOW'];

/** Nombre de cibles par palier, pour la ligne de synthèse. */
export function countByLevel(targets: readonly TargetExposure[]): Record<ExposureLevel, number> {
  const counts: Record<ExposureLevel, number> = { CRITICAL: 0, HIGH: 0, MODERATE: 0, LOW: 0 };
  targets.forEach((target) => {
    counts[target.level] += 1;
  });
  return counts;
}

/**
 * Cible la plus exposée dont le détail est lisible : c'est celle par laquelle
 * commencer. Une cible verrouillée ne peut pas être conseillée — son adresse
 * ne sortira pas.
 */
export function firstDoor(targets: readonly TargetExposure[]): TargetExposure | null {
  return targets.find((target) => !target.locked) ?? null;
}

/** Largeur de barre d'un score, en pourcentage : le score est déjà sur 100. */
export function scoreWidth(score: number): number {
  return Math.max(2, Math.min(100, score));
}
