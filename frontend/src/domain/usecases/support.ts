import { AppError } from '../errors/AppError';
import { validateTicketDraft, type SupportQueue, type Ticket, type TicketDraft, type TicketSummary } from '../models/Support';
import type { SupportRepository } from '../repositories/SupportRepository';

export class ListMyTicketsUseCase {
  constructor(private readonly support: SupportRepository) {}

  execute(): Promise<TicketSummary[]> {
    return this.support.listMine();
  }
}

export class GetTicketUseCase {
  constructor(private readonly support: SupportRepository) {}

  execute(id: number): Promise<Ticket> {
    return this.support.get(id);
  }
}

/** Ouvre une demande, après vérification de la saisie. */
export class OpenTicketUseCase {
  constructor(private readonly support: SupportRepository) {}

  execute(draft: TicketDraft): Promise<Ticket> {
    const errors = validateTicketDraft(draft);
    if (Object.keys(errors).length > 0) return Promise.reject(AppError.validation(errors as Record<string, string>));
    return this.support.open({ ...draft, subject: draft.subject.trim(), body: draft.body.trim() });
  }
}

export class ReplyToTicketUseCase {
  constructor(private readonly support: SupportRepository) {}

  execute(id: number, body: string): Promise<Ticket> {
    const trimmed = body.trim();
    if (!trimmed) return Promise.reject(AppError.validation({ body: 'Le message est vide.' }));
    return this.support.reply(id, trimmed);
  }
}

export class ResolveTicketUseCase {
  constructor(private readonly support: SupportRepository) {}

  execute(id: number): Promise<Ticket> {
    return this.support.resolve(id);
  }
}

export class GetSupportQueueUseCase {
  constructor(private readonly support: SupportRepository) {}

  execute(): Promise<SupportQueue> {
    return this.support.queue();
  }
}
