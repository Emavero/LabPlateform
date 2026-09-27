import { describe, expect, it } from 'vitest';
import {
  countByLevel,
  firstDoor,
  scoreWidth,
  type ExposureLevel,
  type TargetExposure,
} from './Exposure';

function target(slug: string, level: ExposureLevel, score: number, locked = false): TargetExposure {
  return {
    slug,
    name: slug,
    service: { code: 'SSH', label: 'SSH', port: 22, description: 'Accès distant' },
    segment: '10.10.10',
    address: locked ? null : '10.10.10.10',
    score,
    level,
    levelName: level,
    signals: [],
    advice: 'Conseil.',
    locked,
  };
}

describe('lecture de la surface d’attaque', () => {
  it('compte les cibles par palier', () => {
    const counts = countByLevel([
      target('a', 'CRITICAL', 90),
      target('b', 'CRITICAL', 80),
      target('c', 'LOW', 10),
    ]);

    expect(counts).toEqual({ CRITICAL: 2, HIGH: 0, MODERATE: 0, LOW: 1 });
  });

  /** Conseiller une cible verrouillée serait conseiller une adresse qui ne sortira pas. */
  it('ne conseille jamais une cible verrouillée comme première porte', () => {
    const door = firstDoor([target('reservee', 'CRITICAL', 90, true), target('libre', 'HIGH', 60)]);

    expect(door?.slug).toBe('libre');
  });

  it('ne conseille rien quand tout est verrouillé', () => {
    expect(firstDoor([target('reservee', 'CRITICAL', 90, true)])).toBeNull();
  });

  /** Une barre de largeur nulle disparaîtrait : un score bas reste un score. */
  it('garde une barre visible pour un score nul', () => {
    expect(scoreWidth(0)).toBeGreaterThan(0);
    expect(scoreWidth(50)).toBe(50);
    expect(scoreWidth(140)).toBe(100);
  });
});
