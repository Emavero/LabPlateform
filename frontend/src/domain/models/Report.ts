/**
 * Rapport d'activité, vu du navigateur.
 * <p>
 * Le rapport arrive calculé : la seule logique ici est celle de sa lecture
 * (tendances, mise en forme) et de son export.
 */
export interface ReportTotals {
  readonly flags: number;
  readonly boxesPwned: number;
  readonly points: number;
  readonly sections: number;
  readonly quizzesPassed: number;
  readonly quizzesFailed: number;
  readonly minutesStudied: number;
  readonly activeDays: number;
  readonly events: number;
  /** Null quand aucun quiz n'a été rendu : l'affichage rend un tiret, pas un zéro. */
  readonly quizSuccessPercent: number | null;
}

export interface ReportTally {
  readonly code: string;
  readonly label: string;
  readonly count: number;
}

export interface ReportMachineLine {
  readonly slug: string;
  readonly name: string;
  readonly difficulty: string;
  readonly flags: number;
  readonly points: number;
  readonly pwned: boolean;
  readonly lastAt: Date;
}

export interface ReportCourseLine {
  readonly slug: string;
  readonly title: string;
  readonly track: string;
  readonly sections: number;
  readonly minutes: number;
  readonly lastAt: Date;
}

export interface ActivityReport {
  readonly from: Date;
  readonly to: Date;
  readonly days: number;
  readonly totals: ReportTotals;
  readonly previous: ReportTotals;
  readonly families: readonly ReportTally[];
  readonly kinds: readonly ReportTally[];
  readonly machines: readonly ReportMachineLine[];
  readonly courses: readonly ReportCourseLine[];
}

/** Périodes proposées : une semaine, un mois, un trimestre, une année. */
export const REPORT_WINDOWS: readonly number[] = [7, 30, 90, 365];

/** Durée en heures et minutes : « 3 h 20 » se lit mieux que « 200 min ». */
export function hoursAndMinutes(minutes: number): { hours: number; minutes: number } {
  return { hours: Math.floor(minutes / 60), minutes: minutes % 60 };
}

/**
 * Libellés à passer à l'export : ils viennent du catalogue de traduction, de
 * sorte qu'un rapport exporté sort dans la langue de son lecteur.
 */
export interface ReportLabels {
  readonly title: string;
  readonly period: string;
  readonly summary: string;
  readonly flags: string;
  readonly boxes: string;
  readonly points: string;
  readonly sections: string;
  readonly minutes: string;
  readonly activeDays: string;
  readonly quizzes: string;
  readonly families: string;
  readonly machines: string;
  readonly courses: string;
  readonly none: string;
  readonly generated: string;
}

/**
 * Rapport en Markdown, prêt à enregistrer.
 * <p>
 * Markdown et non PDF : le fichier reste lisible tel quel dans un éditeur, se
 * colle dans un message ou une note, et n'ajoute aucune dépendance de mise en
 * page — pour un rapport de quelques dizaines de lignes, c'est le format qui
 * survit le mieux.
 */
export function toMarkdown(report: ActivityReport, labels: ReportLabels, formatDate: (date: Date) => string): string {
  const lines: string[] = [];
  lines.push(`# ${labels.title}`, '');
  lines.push(`${labels.period} : ${formatDate(report.from)} → ${formatDate(report.to)} (${report.days} j)`, '');

  lines.push(`## ${labels.summary}`, '');
  const t = report.totals;
  lines.push(`- ${labels.flags} : ${t.flags}`);
  lines.push(`- ${labels.boxes} : ${t.boxesPwned}`);
  lines.push(`- ${labels.points} : ${t.points}`);
  lines.push(`- ${labels.sections} : ${t.sections}`);
  lines.push(`- ${labels.minutes} : ${t.minutesStudied}`);
  lines.push(`- ${labels.activeDays} : ${t.activeDays}`);
  lines.push(
    `- ${labels.quizzes} : ${t.quizzesPassed} / ${t.quizzesPassed + t.quizzesFailed}` +
      (t.quizSuccessPercent === null ? '' : ` (${t.quizSuccessPercent} %)`),
  );
  lines.push('');

  lines.push(`## ${labels.families}`, '');
  if (report.families.length === 0) {
    lines.push(labels.none);
  } else {
    report.families.forEach((family) => lines.push(`- ${family.label} : ${family.count}`));
  }
  lines.push('');

  lines.push(`## ${labels.machines}`, '');
  if (report.machines.length === 0) {
    lines.push(labels.none);
  } else {
    report.machines.forEach((machine) =>
      lines.push(
        `- ${machine.name} (${machine.difficulty}) — ${machine.flags} × flag, ${machine.points} pts` +
          `, ${formatDate(machine.lastAt)}`,
      ),
    );
  }
  lines.push('');

  lines.push(`## ${labels.courses}`, '');
  if (report.courses.length === 0) {
    lines.push(labels.none);
  } else {
    report.courses.forEach((course) =>
      lines.push(`- ${course.title} (${course.track}) — ${course.sections} × section, ${course.minutes} min`),
    );
  }
  lines.push('', `_${labels.generated}_`, '');

  return lines.join('\n');
}

/** Nom de fichier d'un export : la période le rend reconnaissable dans un dossier. */
export function reportFileName(report: ActivityReport): string {
  return `cyberMans-rapport-${report.to.toISOString().slice(0, 10)}-${report.days}j.md`;
}
