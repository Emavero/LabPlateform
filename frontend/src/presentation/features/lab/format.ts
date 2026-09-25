const TIME = new Intl.DateTimeFormat('fr-FR', { hour: '2-digit', minute: '2-digit' });
const DATE = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'long' });

/** « à 14:32 » le jour même, sinon la date. */
export function formatUptimeSince(date: Date, now: Date = new Date()): string {
  const sameDay = date.toDateString() === now.toDateString();
  return sameDay ? `à ${TIME.format(date)}` : `le ${DATE.format(date)}`;
}

export function formatDate(date: Date): string {
  return DATE.format(date);
}
