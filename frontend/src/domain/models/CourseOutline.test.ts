import { describe, expect, it } from 'vitest';
import { courseOutline, isSubtitle, sectionAnchor, sectionBlocks } from './CourseOutline';
import { firstLabSection } from './Course';
import type { CourseSection } from './Course';

function section(
  slug: string,
  title: string,
  content: string,
  kind: CourseSection['kind'] = 'THEORY',
): CourseSection {
  return {
    id: 1,
    slug,
    title,
    kind,
    kindName: kind,
    position: 1,
    minutes: 10,
    content,
    videoUrl: null,
    completed: false,
    questions: [],
  };
}

describe('sommaire d’un cours', () => {
  it('reconnaît un sous-titre à ses deux dièses', () => {
    expect(isSubtitle('## Capturer la mémoire')).toBe(true);
    expect(isSubtitle('   ## Indenté')).toBe(true);
    expect(isSubtitle('# Un seul dièse')).toBe(false);
    expect(isSubtitle('##Sans espace')).toBe(false);
    expect(isSubtitle('##   ')).toBe(false);
    expect(isSubtitle('Un ## au milieu')).toBe(false);
  });

  it('découpe le contenu en sous-titres et en texte', () => {
    const blocks = sectionBlocks(
      section('capture', 'Capturer', 'Avant tout.\n\n## Outils\nwinpmem -o mem.raw\n\n## Limites\nPas de pagefile.'),
    );

    expect(blocks).toEqual([
      { kind: 'text', text: 'Avant tout.' },
      { kind: 'subtitle', id: 'section-capture-sous-1', label: 'Outils' },
      { kind: 'text', text: 'winpmem -o mem.raw' },
      { kind: 'subtitle', id: 'section-capture-sous-2', label: 'Limites' },
      { kind: 'text', text: 'Pas de pagefile.' },
    ]);
  });

  it('préserve l’indentation du texte, qui porte des commandes', () => {
    const blocks = sectionBlocks(section('s', 'S', '## Étapes\n  1. lancer\n     2. vérifier'));

    expect(blocks[1]).toEqual({ kind: 'text', text: '  1. lancer\n     2. vérifier' });
  });

  it('ne fabrique pas de bloc pour un contenu vide', () => {
    expect(sectionBlocks(section('s', 'S', '   \n\n  '))).toEqual([]);
    expect(sectionBlocks(section('s', 'S', ''))).toEqual([]);
  });

  it('liste les sections en niveau 2 et leurs sous-titres en niveau 3', () => {
    const outline = courseOutline({
      sections: [
        section('capture', 'Capturer sans altérer', 'Texte.\n## Outils\nDétail.'),
        section('analyse', 'Analyser', 'Sans sous-titre.'),
      ],
    });

    expect(outline).toEqual([
      { id: 'section-capture', label: 'Capturer sans altérer', level: 2 },
      { id: 'section-capture-sous-1', label: 'Outils', level: 3 },
      { id: 'section-analyse', label: 'Analyser', level: 2 },
    ]);
  });

  it('donne à chaque entrée une ancre distincte', () => {
    const outline = courseOutline({
      sections: [
        section('a', 'A', '## Même titre\n## Même titre'),
        section('b', 'B', '## Même titre'),
      ],
    });

    expect(new Set(outline.map((entry) => entry.id)).size).toBe(outline.length);
  });

  it('ancre une section sur son slug, pour que les liens partagés survivent', () => {
    expect(sectionAnchor({ slug: 'chaine-de-possession' })).toBe('section-chaine-de-possession');
  });
});

describe('atelier d’un cours', () => {
  it('trouve la première section d’atelier', () => {
    const course = {
      sections: [
        section('intro', 'Intro', 'Texte.'),
        section('atelier-1', 'Atelier 1', 'Commandes.', 'LAB'),
        section('atelier-2', 'Atelier 2', 'Encore.', 'LAB'),
      ],
    };

    // La première seulement : le bouton de machine ne doit pas se répéter, chaque
    // copie interrogeant le serveur pour le même état.
    expect(firstLabSection(course)?.slug).toBe('atelier-1');
  });

  it('ne trouve rien quand le cours est entièrement théorique', () => {
    const course = { sections: [section('intro', 'Intro', 'Texte.')] };

    expect(firstLabSection(course)).toBeUndefined();
  });
});
