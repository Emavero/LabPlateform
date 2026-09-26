import { describe, expect, it } from 'vitest';
import { familiesOf, filterByFamily, groupByDay, type JournalLine } from './Journal';

function line(kind: string, family: JournalLine['family'], at: string): JournalLine {
  return {
    kind,
    kindName: kind,
    family,
    familyName: family,
    handle: 'alice',
    subject: null,
    detail: null,
    at: new Date(at),
  };
}

const lines: JournalLine[] = [
  line('BOX_PWNED', 'MACHINES', '2026-09-25T18:00:00Z'),
  line('COURSE_VIEWED', 'ACADEMY', '2026-09-25T09:00:00Z'),
  line('REGISTERED', 'ACCOUNT', '2026-09-20T08:00:00Z'),
];

describe('Journal', () => {
  it('ne propose en filtre que les familles réellement présentes, dans l’ordre d’affichage', () => {
    // ACCOUNT passe en dernier : l'inscription intéresse moins que l'usage.
    expect(familiesOf(lines)).toEqual(['MACHINES', 'ACADEMY', 'ACCOUNT']);
  });

  it('filtre par famille, et rend tout sans filtre', () => {
    expect(filterByFamily(lines, 'ACADEMY')).toHaveLength(1);
    expect(filterByFamily(lines, null)).toHaveLength(3);
  });

  it('regroupe par journée, la plus récente d’abord', () => {
    const days = groupByDay(lines);

    expect(days.map((day) => day.day)).toEqual(['2026-09-25', '2026-09-20']);
    expect(days[0].lines).toHaveLength(2);
  });

  it('ne rend aucune journée pour un journal vide', () => {
    expect(groupByDay([])).toEqual([]);
  });
});
