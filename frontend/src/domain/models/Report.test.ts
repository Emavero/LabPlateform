import { describe, expect, it } from 'vitest';
import {
  hoursAndMinutes,
  reportFileName,
  toMarkdown,
  type ActivityReport,
  type ReportLabels,
  type ReportTotals,
} from './Report';

const labels: ReportLabels = {
  title: 'Rapport',
  period: 'Période',
  summary: 'Synthèse',
  flags: 'Flags',
  boxes: 'Machines',
  points: 'Points',
  sections: 'Sections',
  minutes: 'Minutes',
  activeDays: 'Jours actifs',
  quizzes: 'Quiz',
  families: 'Familles',
  machines: 'Machines de la période',
  courses: 'Cours',
  none: 'Rien.',
  generated: 'Établi par cyberMans.',
};

const totals = (changes: Partial<ReportTotals> = {}): ReportTotals => ({
  flags: 4,
  boxesPwned: 2,
  points: 40,
  sections: 6,
  quizzesPassed: 3,
  quizzesFailed: 1,
  minutesStudied: 200,
  activeDays: 5,
  events: 30,
  quizSuccessPercent: 75,
  ...changes,
});

const report = (changes: Partial<ActivityReport> = {}): ActivityReport => ({
  from: new Date('2026-08-28T12:00:00Z'),
  to: new Date('2026-09-27T12:00:00Z'),
  days: 30,
  totals: totals(),
  previous: totals({ flags: 2 }),
  families: [{ code: 'MACHINES', label: 'Machines', count: 20 }],
  kinds: [{ code: 'BOX_VIEWED', label: 'Machine consultée', count: 12 }],
  machines: [
    {
      slug: 'sentinel',
      name: 'Sentinel',
      difficulty: 'Facile',
      flags: 2,
      points: 20,
      pwned: true,
      lastAt: new Date('2026-09-20T12:00:00Z'),
    },
  ],
  courses: [
    {
      slug: 'triage',
      title: 'Triage mémoire',
      track: 'Forensique',
      sections: 3,
      minutes: 90,
      lastAt: new Date('2026-09-18T12:00:00Z'),
    },
  ],
  ...changes,
});

const iso = (date: Date) => date.toISOString().slice(0, 10);

describe('lecture d’un rapport', () => {
  it('rend une durée en heures et minutes', () => {
    expect(hoursAndMinutes(200)).toEqual({ hours: 3, minutes: 20 });
    expect(hoursAndMinutes(45)).toEqual({ hours: 0, minutes: 45 });
    expect(hoursAndMinutes(0)).toEqual({ hours: 0, minutes: 0 });
  });

  it('nomme le fichier par sa date et sa durée', () => {
    expect(reportFileName(report())).toBe('cyberMans-rapport-2026-09-27-30j.md');
  });
});

describe('export d’un rapport', () => {
  it('reprend les intitulés qu’on lui donne, donc la langue du lecteur', () => {
    const markdown = toMarkdown(report(), { ...labels, title: 'Report', flags: 'Flags validated' }, iso);

    expect(markdown).toContain('# Report');
    expect(markdown).toContain('- Flags validated : 4');
  });

  it('porte la période, les totaux, les machines et les cours', () => {
    const markdown = toMarkdown(report(), labels, iso);

    expect(markdown).toContain('2026-08-28 → 2026-09-27');
    expect(markdown).toContain('- Machines : 2');
    expect(markdown).toContain('Sentinel (Facile) — 2 × flag, 20 pts');
    expect(markdown).toContain('Triage mémoire (Forensique) — 3 × section, 90 min');
    expect(markdown).toContain('Établi par cyberMans.');
  });

  it('affiche la part de réussite aux quiz quand elle existe', () => {
    expect(toMarkdown(report(), labels, iso)).toContain('- Quiz : 3 / 4 (75 %)');
  });

  /** Sans quiz rendu, il n'y a pas de pourcentage à écrire — pas même zéro. */
  it('omet la part de réussite quand aucun quiz n’a été rendu', () => {
    const sans = report({ totals: totals({ quizzesPassed: 0, quizzesFailed: 0, quizSuccessPercent: null }) });

    const markdown = toMarkdown(sans, labels, iso);

    expect(markdown).toContain('- Quiz : 0 / 0');
    expect(markdown).not.toContain('%)');
  });

  it('dit « rien » plutôt que de laisser une section vide', () => {
    const vide = report({ families: [], machines: [], courses: [] });

    const markdown = toMarkdown(vide, labels, iso);

    expect(markdown.match(/Rien\./g)).toHaveLength(3);
  });
});
