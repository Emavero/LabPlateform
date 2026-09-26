export type ActivityKind = 'FLAG' | 'SECTION';

/** Haut fait, obtenu ou non : les manquants disent quoi viser. */
export interface Achievement {
  readonly code: string;
  readonly name: string;
  readonly requirement: string;
  readonly earned: boolean;
}

export interface ActivityEntry {
  readonly kind: ActivityKind;
  readonly title: string;
  readonly detail: string;
  readonly points: number;
  readonly firstBlood: boolean;
  readonly at: Date;
}

export function earnedCount(achievements: readonly Achievement[]): number {
  return achievements.filter((achievement) => achievement.earned).length;
}
