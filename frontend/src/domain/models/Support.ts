/**
 * Assistance vue du navigateur.
 * <p>
 * Les statuts et les catégories sont des codes, jamais des libellés : le nom
 * affiché vient du catalogue de traduction, et le serveur envoie de son côté le
 * même nom déjà traduit pour les lecteurs qui n'ont pas notre catalogue.
 */
export type TicketStatus = 'OPEN' | 'ANSWERED' | 'RESOLVED';

export type TicketCategory = 'ACCOUNT' | 'BILLING' | 'MACHINES' | 'COURSES' | 'LAB' | 'OTHER';

/** Catégories dans l'ordre où le formulaire les propose. */
export const TICKET_CATEGORIES: readonly TicketCategory[] = [
  'ACCOUNT',
  'BILLING',
  'MACHINES',
  'COURSES',
  'LAB',
  'OTHER',
];

export interface TicketMessage {
  readonly fromStaff: boolean;
  readonly body: string;
  readonly sentAt: Date;
}

/** Ligne de liste : de quoi choisir quelle demande ouvrir, sans son fil. */
export interface TicketSummary {
  readonly id: number;
  readonly handle: string;
  readonly mine: boolean;
  readonly category: TicketCategory;
  readonly categoryName: string;
  readonly subject: string;
  readonly status: TicketStatus;
  readonly statusName: string;
  readonly messages: number;
  readonly lastMessage: string | null;
  readonly lastFromStaff: boolean;
  readonly createdAt: Date;
  readonly updatedAt: Date;
}

export interface Ticket {
  readonly id: number;
  readonly handle: string;
  readonly mine: boolean;
  readonly category: TicketCategory;
  readonly categoryName: string;
  readonly subject: string;
  readonly status: TicketStatus;
  readonly statusName: string;
  readonly createdAt: Date;
  readonly updatedAt: Date;
  readonly messages: readonly TicketMessage[];
}

export interface TicketDraft {
  readonly category: TicketCategory;
  readonly subject: string;
  readonly body: string;
}

export const EMPTY_TICKET_DRAFT: TicketDraft = { category: 'ACCOUNT', subject: '', body: '' };

/** File d'attente de l'équipe, classée par ce que chaque demande attend. */
export interface SupportQueue {
  readonly waiting: readonly TicketSummary[];
  readonly answered: readonly TicketSummary[];
  readonly resolved: readonly TicketSummary[];
  readonly byCategory: readonly CategoryTally[];
  /** Délai médian de première réponse, ou null tant qu'aucune demande n'a de réponse. */
  readonly medianMinutesToFirstReply: number | null;
}

export interface CategoryTally {
  readonly category: TicketCategory;
  readonly categoryName: string;
  readonly count: number;
}

export const SUBJECT_MAX_LENGTH = 140;
export const BODY_MAX_LENGTH = 8_000;

/**
 * Règles de saisie, miroir de celles du serveur qui reste l'autorité : elles
 * évitent un aller-retour pour un formulaire vide.
 */
export function validateTicketDraft(draft: TicketDraft): Partial<Record<'subject' | 'body', string>> {
  const errors: Partial<Record<'subject' | 'body', string>> = {};
  const subject = draft.subject.trim();
  if (!subject) {
    errors.subject = 'Le sujet est obligatoire.';
  } else if (subject.length > SUBJECT_MAX_LENGTH) {
    errors.subject = `Le sujet est limité à ${SUBJECT_MAX_LENGTH} caractères.`;
  }
  const body = draft.body.trim();
  if (!body) {
    errors.body = 'Le message est vide.';
  } else if (body.length > BODY_MAX_LENGTH) {
    errors.body = `Le message est limité à ${BODY_MAX_LENGTH} caractères.`;
  }
  return errors;
}

/** Une demande attend l'équipe : c'est ce qui compose sa file. */
export function isWaitingOnStaff(ticket: { status: TicketStatus }): boolean {
  return ticket.status === 'OPEN';
}

/** Nombre total de demandes de la file, tous états confondus. */
export function queueSize(queue: SupportQueue): number {
  return queue.waiting.length + queue.answered.length + queue.resolved.length;
}
