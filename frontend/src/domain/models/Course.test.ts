import { describe, expect, it } from 'vitest';
import { courseRatio, formatDuration, nextSection, type Course, type CourseSection } from './Course';

function section(position: number, completed: boolean): CourseSection {
  return {
    slug: `section-${position}`,
    title: `Section ${position}`,
    kind: 'THEORY',
    kindName: 'Cours',
    position,
    minutes: 15,
    content: 'Contenu',
    completed,
  };
}

const course: Course = {
  slug: 'traces',
  title: 'Traces',
  track: 'FORENSICS',
  trackName: 'Forensique',
  trackSlug: 'forensique',
  level: 'FUNDAMENTAL',
  levelName: 'Fondamental',
  summary: 'Résumé.',
  minutes: 45,
  sectionsCompleted: 1,
  completed: false,
  publishedAt: new Date('2026-09-01T00:00:00Z'),
  sections: [section(1, true), section(2, false), section(3, false)],
};

describe('avancement dans un cours', () => {
  it('rapporte les sections terminées sur le total', () => {
    expect(courseRatio({ sections: 3, sectionsCompleted: 1 })).toBeCloseTo(1 / 3);
    expect(courseRatio({ sections: 4, sectionsCompleted: 4 })).toBe(1);
  });

  it('ne divise pas par zéro sur un cours vide', () => {
    expect(courseRatio({ sections: 0, sectionsCompleted: 0 })).toBe(0);
  });

  it('reprend à la première section non terminée', () => {
    expect(nextSection(course)?.slug).toBe('section-2');
  });

  it('ne propose rien à reprendre sur un cours fini', () => {
    const finished = { ...course, sections: [section(1, true), section(2, true)] };
    expect(nextSection(finished)).toBeUndefined();
  });
});

describe('formatDuration', () => {
  it('reste en minutes sous une heure', () => {
    expect(formatDuration(0)).toBe('0 min');
    expect(formatDuration(45)).toBe('45 min');
  });

  it('passe en heures au-delà', () => {
    expect(formatDuration(60)).toBe('1 h');
    expect(formatDuration(80)).toBe('1 h 20');
    expect(formatDuration(125)).toBe('2 h 05');
  });
});
