import { describe, expect, it } from 'vitest';
import {
  BODY_MAX_LENGTH,
  EMPTY_TICKET_DRAFT,
  SUBJECT_MAX_LENGTH,
  isWaitingOnStaff,
  queueSize,
  validateTicketDraft,
  type SupportQueue,
  type TicketSummary,
} from './Support';

describe('saisie d’une demande', () => {
  it('refuse un sujet et un message vides', () => {
    const errors = validateTicketDraft(EMPTY_TICKET_DRAFT);

    expect(errors.subject).toBeDefined();
    expect(errors.body).toBeDefined();
  });

  /** Des espaces ne sont pas un message : le serveur les refuserait aussi. */
  it('ne prend pas des espaces pour une saisie', () => {
    const errors = validateTicketDraft({ category: 'OTHER', subject: '   ', body: '\n\t ' });

    expect(errors.subject).toBeDefined();
    expect(errors.body).toBeDefined();
  });

  it('accepte une demande renseignée', () => {
    const errors = validateTicketDraft({ category: 'BILLING', subject: 'Paiement refusé', body: 'Étape 2.' });

    expect(errors).toEqual({});
  });

  it('borne le sujet et le message', () => {
    const errors = validateTicketDraft({
      category: 'OTHER',
      subject: 's'.repeat(SUBJECT_MAX_LENGTH + 1),
      body: 'b'.repeat(BODY_MAX_LENGTH + 1),
    });

    expect(errors.subject).toContain(String(SUBJECT_MAX_LENGTH));
    expect(errors.body).toContain(String(BODY_MAX_LENGTH));
  });
});

describe('lecture d’une file', () => {
  const summary = (id: number, status: TicketSummary['status']): TicketSummary => ({
    id,
    handle: 'alice',
    mine: false,
    category: 'LAB',
    categoryName: 'Infrastructure du lab',
    subject: 'Sujet',
    status,
    statusName: 'Ouverte',
    messages: 1,
    lastMessage: 'Message',
    lastFromStaff: false,
    createdAt: new Date('2026-09-27T10:00:00Z'),
    updatedAt: new Date('2026-09-27T10:00:00Z'),
  });

  it('reconnaît une demande qui attend l’équipe', () => {
    expect(isWaitingOnStaff(summary(1, 'OPEN'))).toBe(true);
    expect(isWaitingOnStaff(summary(2, 'ANSWERED'))).toBe(false);
    expect(isWaitingOnStaff(summary(3, 'RESOLVED'))).toBe(false);
  });

  it('compte toutes les demandes de la file', () => {
    const queue: SupportQueue = {
      waiting: [summary(1, 'OPEN'), summary(2, 'OPEN')],
      answered: [summary(3, 'ANSWERED')],
      resolved: [],
      byCategory: [],
      medianMinutesToFirstReply: null,
    };

    expect(queueSize(queue)).toBe(3);
  });
});
