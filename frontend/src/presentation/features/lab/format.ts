/**
 * Deux dates tombent-elles le même jour ? La formulation (« à 14:32 » ou « le
 * 3 mars ») et le format viennent du catalogue et de la locale, pas d'ici :
 * ce fichier ne connaît plus aucune langue.
 */
export function isSameDay(a: Date, b: Date): boolean {
  return a.toDateString() === b.toDateString();
}
