import type { SupportQueue, Ticket, TicketDraft, TicketSummary } from '../models/Support';

export interface SupportRepository {
  /** Demandes de l'appelant. */
  listMine(): Promise<TicketSummary[]>;
  get(id: number): Promise<Ticket>;
  open(draft: TicketDraft): Promise<Ticket>;
  reply(id: number, body: string): Promise<Ticket>;
  resolve(id: number): Promise<Ticket>;
  /** File de l'équipe : refusée par le serveur à qui n'est pas administrateur. */
  queue(): Promise<SupportQueue>;
}
