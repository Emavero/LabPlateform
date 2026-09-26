import { describe, expect, it } from 'vitest';
import { ratingGap, type Box } from './Box';

const box: Box = {
  slug: 'mirage',
  name: 'Mirage',
  os: 'LINUX',
  osName: 'Ubuntu',
  difficulty: 'MEDIUM',
  difficultyName: 'Moyenne',
  userFlagPoints: 12,
  rootFlagPoints: 18,
  totalPoints: 30,
  synopsis: 'Synopsis.',
  ipAddress: '10.10.10.14',
  maker: 'cyberMans',
  releasedAt: new Date('2026-09-01T00:00:00Z'),
  retired: false,
  userOwned: false,
  rootOwned: false,
  pwned: false,
  firstBlood: false,
  pointsEarned: 0,
  lastOwnedAt: null,
  ratingVotes: 0,
  ratingAverage: 0,
  perceivedDifficulty: null,
  perceivedDifficultyName: null,
  myRating: null,
};

describe('ratingGap', () => {
  it('vaut zéro sans vote', () => {
    expect(ratingGap(box)).toBe(0);
  });

  it('mesure en paliers l’écart avec la difficulté annoncée', () => {
    expect(ratingGap({ ...box, ratingVotes: 3, perceivedDifficulty: 'HARD' })).toBe(1);
    expect(ratingGap({ ...box, ratingVotes: 3, perceivedDifficulty: 'VERY_EASY' })).toBe(-2);
    expect(ratingGap({ ...box, ratingVotes: 3, perceivedDifficulty: 'MEDIUM' })).toBe(0);
  });
});
