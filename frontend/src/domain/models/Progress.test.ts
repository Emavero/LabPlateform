import { describe, expect, it } from 'vitest';
import { completionPercent, progressToNextRank, type PlayerProgress } from './Progress';

const progress: PlayerProgress = {
  points: 10,
  availablePoints: 200,
  ownedFlags: 1,
  totalFlags: 12,
  boxesPwned: 0,
  firstBloods: 0,
  rank: 'SCRIPT_KIDDIE',
  rankName: 'Script Kiddie',
  nextRank: 'HACKER',
  nextRankName: 'Hacker',
  pointsToNextRank: 20,
  completion: 0.05,
};

describe('progression', () => {
  it('mesure le chemin parcouru vers le rang suivant', () => {
    // 10 points acquis sur les 30 qu'il faut atteindre pour devenir Hacker.
    expect(progressToNextRank(progress)).toBeCloseTo(1 / 3);
  });

  it('affiche une barre pleine au dernier rang', () => {
    const top = { ...progress, rank: 'OMNISCIENT', nextRank: null, nextRankName: null, pointsToNextRank: 0 } as const;
    expect(progressToNextRank(top)).toBe(1);
  });

  it('reste à zéro pour un joueur sans point', () => {
    expect(progressToNextRank({ ...progress, points: 0, pointsToNextRank: 0 })).toBe(0);
  });

  it('exprime la part du catalogue possédée en pourcentage', () => {
    expect(completionPercent(progress)).toBe(5);
    expect(completionPercent({ ...progress, completion: 0.356 })).toBe(36);
  });
});
