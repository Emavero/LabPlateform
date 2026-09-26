/**
 * Journal d'activité : ce que le compte a fait, tel que le serveur l'a inscrit.
 * Rien n'est mesuré dans le navigateur — ni pixel, ni balise de suivi : une
 * ligne du journal correspond à une action réellement demandée à la plateforme.
 */
export type JournalFamily = 'ACCOUNT' | 'MACHINES' | 'ACADEMY' | 'LAB' | 'BILLING' | 'SUPPORT';

export interface JournalLine {
  readonly kind: string;
  /** Libellé envoyé par le serveur : le client n'en tient pas la table. */
  readonly kindName: string;
  readonly family: JournalFamily;
  readonly familyName: string;
  readonly handle: string;
  /** Ressource concernée, par son lien : machine, cours, moyen de paiement. */
  readonly subject: string | null;
  readonly detail: string | null;
  readonly at: Date;
}

export const FAMILY_ORDER: readonly JournalFamily[] = [
  'MACHINES',
  'ACADEMY',
  'LAB',
  'BILLING',
  'SUPPORT',
  'ACCOUNT',
];

/** Familles réellement présentes, dans l'ordre d'affichage : sert aux filtres. */
export function familiesOf(lines: readonly JournalLine[]): JournalFamily[] {
  const present = new Set(lines.map((line) => line.family));
  return FAMILY_ORDER.filter((family) => present.has(family));
}

export function filterByFamily(lines: readonly JournalLine[], family: JournalFamily | null): JournalLine[] {
  return family === null ? [...lines] : lines.filter((line) => line.family === family);
}

/**
 * Regroupe par jour, du plus récent au plus ancien. Un journal se lit par
 * journées : une liste plate de trois cents lignes ne se lit pas.
 */
export function groupByDay(lines: readonly JournalLine[]): { day: string; lines: JournalLine[] }[] {
  const byDay = new Map<string, JournalLine[]>();
  for (const line of lines) {
    const day = line.at.toISOString().slice(0, 10);
    const bucket = byDay.get(day);
    if (bucket) bucket.push(line);
    else byDay.set(day, [line]);
  }
  return [...byDay.entries()]
    .sort(([left], [right]) => right.localeCompare(left))
    .map(([day, group]) => ({ day, lines: group }));
}
