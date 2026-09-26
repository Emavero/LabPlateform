import { describe, expect, it } from 'vitest';
import { earnedCount, type Achievement } from './Profile';

const achievement = (code: string, earned: boolean): Achievement => ({
  code,
  name: code,
  requirement: 'Condition',
  earned,
});

describe('earnedCount', () => {
  it('ne compte que les hauts faits obtenus', () => {
    expect(earnedCount([achievement('A', true), achievement('B', false), achievement('C', true)])).toBe(2);
    expect(earnedCount([])).toBe(0);
  });
});
