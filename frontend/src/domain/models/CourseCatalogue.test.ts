import { describe, expect, it } from 'vitest';
import { filterCourses, NO_FILTER, topicsOf, topicStillValid } from './CourseCatalogue';
import type { CourseSummary, Topic, TopicCode, Track, TrackCode } from './Course';

function topic(code: TopicCode, slug: string, track: TrackCode): Topic {
  return { topic: code, slug, name: slug, track };
}

const FORENSICS: Track = {
  track: 'FORENSICS',
  slug: 'forensique',
  name: 'Forensique',
  description: '',
  topics: [
    topic('MEMORY_ANALYSIS', 'analyse-memoire', 'FORENSICS'),
    topic('DISK_FORENSICS', 'analyse-de-disque', 'FORENSICS'),
    topic('LOG_ANALYSIS', 'analyse-de-journaux', 'FORENSICS'),
  ],
};

const DEFENSE: Track = {
  track: 'DEFENSE',
  slug: 'defense',
  name: 'Défense',
  description: '',
  topics: [topic('HARDENING', 'durcissement', 'DEFENSE'), topic('SIEM_SOC', 'siem-et-soc', 'DEFENSE')],
};

const TRACKS = [FORENSICS, DEFENSE];

function course(slug: string, trackSlug: string, topicSlug: string): CourseSummary {
  return {
    slug,
    title: slug,
    track: trackSlug === 'forensique' ? 'FORENSICS' : 'DEFENSE',
    trackName: trackSlug,
    trackSlug,
    topic: 'MEMORY_ANALYSIS',
    topicName: topicSlug,
    topicSlug,
    level: 'EASY',
    levelName: 'Facile',
    summary: '',
    sections: 1,
    sectionsCompleted: 0,
    minutes: 10,
    completed: false,
    started: false,
    publishedAt: new Date('2026-01-01T00:00:00Z'),
  };
}

const CATALOGUE = [
  course('memoire-1', 'forensique', 'analyse-memoire'),
  course('memoire-2', 'forensique', 'analyse-memoire'),
  course('disque-1', 'forensique', 'analyse-de-disque'),
  course('durcir-1', 'defense', 'durcissement'),
];

describe('catalogue des cours', () => {
  it('sans filtre, montre tout le catalogue', () => {
    expect(filterCourses(CATALOGUE, NO_FILTER)).toHaveLength(4);
  });

  it('filtre par filière', () => {
    expect(filterCourses(CATALOGUE, { trackSlug: 'forensique', topicSlug: null })).toHaveLength(3);
    expect(filterCourses(CATALOGUE, { trackSlug: 'defense', topicSlug: null })).toHaveLength(1);
  });

  it('filtre par sous-domaine à l’intérieur d’une filière', () => {
    const found = filterCourses(CATALOGUE, { trackSlug: 'forensique', topicSlug: 'analyse-memoire' });

    expect(found.map((c) => c.slug)).toEqual(['memoire-1', 'memoire-2']);
  });

  it('ne propose aucun sous-domaine tant qu’aucune filière n’est choisie', () => {
    expect(topicsOf(TRACKS, CATALOGUE, null)).toEqual([]);
  });

  it('propose les sous-domaines de la filière, avec leur nombre de cours', () => {
    expect(topicsOf(TRACKS, CATALOGUE, 'forensique').map((e) => [e.topic.slug, e.courses])).toEqual([
      ['analyse-memoire', 2],
      ['analyse-de-disque', 1],
    ]);
  });

  it('écarte un sous-domaine qui ne mènerait qu’à une page vide', () => {
    // « analyse-de-journaux » existe côté serveur mais n'a aucun cours.
    expect(topicsOf(TRACKS, CATALOGUE, 'forensique').map((e) => e.topic.slug)).not.toContain(
      'analyse-de-journaux',
    );
  });

  it('garde l’ordre des sous-domaines voulu par le serveur', () => {
    const reversed = [...CATALOGUE].reverse();

    expect(topicsOf(TRACKS, reversed, 'forensique').map((e) => e.topic.slug)).toEqual([
      'analyse-memoire',
      'analyse-de-disque',
    ]);
  });

  it('ignore une filière inconnue', () => {
    expect(topicsOf(TRACKS, CATALOGUE, 'cryptographie')).toEqual([]);
  });

  it('oublie un sous-domaine qui n’appartient pas à la filière choisie', () => {
    expect(topicStillValid(TRACKS, 'forensique', 'analyse-memoire')).toBe(true);
    expect(topicStillValid(TRACKS, 'defense', 'analyse-memoire')).toBe(false);
    expect(topicStillValid(TRACKS, null, 'analyse-memoire')).toBe(false);
    expect(topicStillValid(TRACKS, 'forensique', null)).toBe(true);
  });
});
