import type { Course, CourseSection } from './Course';

/** Entrée du sommaire : un titre de section (H2) ou un sous-titre (H3). */
export interface OutlineEntry {
  /** Ancre de la cible dans la page : le lien et le titre la partagent. */
  readonly id: string;
  readonly label: string;
  readonly level: 2 | 3;
}

/** Contenu d'une section, découpé en sous-titres et en blocs de texte. */
export type ContentBlock =
  | { readonly kind: 'subtitle'; readonly id: string; readonly label: string }
  | { readonly kind: 'text'; readonly text: string };

/**
 * Sous-titre dans le contenu d'une section : une ligne qui commence par `##`.
 * <p>
 * Le titre de la section est déjà le H2 de la page ; ce qui est écrit dedans
 * vient donc un niveau plus bas. Deux dièses plutôt qu'un, parce qu'un seul
 * suggérerait un titre de même rang que celui de la section.
 */
const SUBTITLE = /^\s*##\s+(\S.*?)\s*$/;

export function isSubtitle(line: string): boolean {
  return SUBTITLE.test(line);
}

/**
 * Ancre d'une section. Dérivée du slug, donc stable : un lien vers une section
 * partagé aujourd'hui pointe encore au bon endroit demain.
 */
export function sectionAnchor(section: Pick<CourseSection, 'slug'>): string {
  return `section-${section.slug}`;
}

function subtitleAnchor(section: Pick<CourseSection, 'slug'>, index: number): string {
  return `${sectionAnchor(section)}-sous-${index}`;
}

/**
 * Découpe le contenu d'une section pour l'affichage.
 * <p>
 * Le texte hors sous-titre reste préformaté : les commandes et les extraits de
 * journaux doivent garder leurs espaces et leurs retours à la ligne. Seules les
 * lignes de sous-titre en sortent, pour devenir des titres que le sommaire peut
 * viser.
 */
export function sectionBlocks(section: Pick<CourseSection, 'slug' | 'content'>): ContentBlock[] {
  const blocks: ContentBlock[] = [];
  const buffer: string[] = [];
  let subtitles = 0;

  const flush = () => {
    // Les lignes vides autour d'un sous-titre ne forment pas un bloc de texte.
    const text = buffer.join('\n').replace(/^\n+|\n+$/g, '');
    if (text.trim()) blocks.push({ kind: 'text', text });
    buffer.length = 0;
  };

  for (const line of section.content.split('\n')) {
    const match = SUBTITLE.exec(line);
    if (match) {
      flush();
      subtitles += 1;
      blocks.push({ kind: 'subtitle', id: subtitleAnchor(section, subtitles), label: match[1] });
    } else {
      buffer.push(line);
    }
  }
  flush();
  return blocks;
}

/**
 * Sommaire du cours, tiré de son contenu et de lui seul.
 * <p>
 * Chaque section donne un titre de niveau 2, chaque sous-titre écrit dans son
 * contenu un titre de niveau 3. Rien n'est saisi à part : un sommaire tenu à la
 * main finit toujours par mentir sur ce que la page contient.
 * <p>
 * Les sections ajoutées par la page — chemin d'attaque, cas d'usage,
 * concepteurs — n'apparaissent pas ici : leurs intitulés sont traduits, et la
 * traduction n'appartient pas au domaine. C'est la page qui les ajoute.
 */
export function courseOutline(course: Pick<Course, 'sections'>): OutlineEntry[] {
  return course.sections.flatMap((section) => [
    { id: sectionAnchor(section), label: section.title, level: 2 as const },
    ...sectionBlocks(section)
      .filter((block): block is Extract<ContentBlock, { kind: 'subtitle' }> => block.kind === 'subtitle')
      .map((block) => ({ id: block.id, label: block.label, level: 3 as const })),
  ]);
}
