import { describe, expect, it } from 'vitest';
import {
  courseRatio,
  formatDuration,
  hasQuiz,
  isQuizComplete,
  nextSection,
  type Course,
  type CourseSection,
} from './Course';

function section(position: number, completed: boolean): CourseSection {
  return {
    id: position,
    slug: `section-${position}`,
    title: `Section ${position}`,
    kind: 'THEORY',
    kindName: 'Cours',
    position,
    minutes: 15,
    content: 'Contenu',
    videoUrl: null,
    completed,
    questions: [],
  };
}

const course: Course = {
  slug: 'traces',
  title: 'Traces',
  track: 'FORENSICS',
  trackName: 'Forensique',
  trackSlug: 'forensique',
  topic: 'EVIDENCE_HANDLING',
  topicName: 'Collecte et preuve',
  topicSlug: 'collecte-et-preuve',
  level: 'FUNDAMENTAL',
  levelName: 'Fondamental',
  summary: 'Résumé.',
  minutes: 45,
  sectionsCompleted: 1,
  completed: false,
  publishedAt: new Date('2026-09-01T00:00:00Z'),
  attackPath: null,
  realCase: null,
  designers: [],
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

describe('quiz', () => {
  const question = (id: number) => ({
    id,
    statement: `Question ${id}`,
    position: id,
    choices: [
      { id: id * 10, label: 'A', correct: null },
      { id: id * 10 + 1, label: 'B', correct: null },
    ],
  });
  const quizSection = { ...section(1, false), questions: [question(1), question(2)] };

  it('reconnaît une section à quiz', () => {
    expect(hasQuiz(quizSection)).toBe(true);
    expect(hasQuiz(section(1, false))).toBe(false);
  });

  it('exige une réponse à chaque question avant de rendre la copie', () => {
    expect(isQuizComplete(quizSection, {})).toBe(false);
    expect(isQuizComplete(quizSection, { 1: [10] })).toBe(false);
    expect(isQuizComplete(quizSection, { 1: [10], 2: [21] })).toBe(true);
    // Une question décochée ne compte pas comme répondue.
    expect(isQuizComplete(quizSection, { 1: [10], 2: [] })).toBe(false);
  });
});
