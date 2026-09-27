import type { AxiosInstance } from 'axios';
import type {
  SupportQueue,
  Ticket,
  TicketDraft,
  TicketMessage,
  TicketSummary,
} from '@/domain/models/Support';
import type { SupportRepository } from '@/domain/repositories/SupportRepository';

type Dated<T, K extends keyof T> = Omit<T, K> & Record<Extract<K, string>, string>;
type MessageDto = Dated<TicketMessage, 'sentAt'>;
type SummaryDto = Dated<TicketSummary, 'createdAt' | 'updatedAt'>;
type TicketDto = Omit<Dated<Ticket, 'createdAt' | 'updatedAt'>, 'messages'> & { messages: MessageDto[] };
type QueueDto = Omit<SupportQueue, 'waiting' | 'answered' | 'resolved'> & {
  waiting: SummaryDto[];
  answered: SummaryDto[];
  resolved: SummaryDto[];
};

export class HttpSupportRepository implements SupportRepository {
  constructor(private readonly http: AxiosInstance) {}

  async listMine(): Promise<TicketSummary[]> {
    const { data } = await this.http.get<SummaryDto[]>('/support/tickets');
    return data.map(toSummary);
  }

  async get(id: number): Promise<Ticket> {
    const { data } = await this.http.get<TicketDto>(`/support/tickets/${id}`);
    return toTicket(data);
  }

  async open(draft: TicketDraft): Promise<Ticket> {
    const { data } = await this.http.post<TicketDto>('/support/tickets', draft);
    return toTicket(data);
  }

  async reply(id: number, body: string): Promise<Ticket> {
    const { data } = await this.http.post<TicketDto>(`/support/tickets/${id}/messages`, { body });
    return toTicket(data);
  }

  async resolve(id: number): Promise<Ticket> {
    const { data } = await this.http.post<TicketDto>(`/support/tickets/${id}/resolution`);
    return toTicket(data);
  }

  async queue(): Promise<SupportQueue> {
    const { data } = await this.http.get<QueueDto>('/admin/support/queue');
    return {
      ...data,
      waiting: data.waiting.map(toSummary),
      answered: data.answered.map(toSummary),
      resolved: data.resolved.map(toSummary),
    };
  }
}

function toSummary(dto: SummaryDto): TicketSummary {
  return { ...dto, createdAt: new Date(dto.createdAt), updatedAt: new Date(dto.updatedAt) };
}

function toTicket(dto: TicketDto): Ticket {
  return {
    ...dto,
    createdAt: new Date(dto.createdAt),
    updatedAt: new Date(dto.updatedAt),
    messages: dto.messages.map((message) => ({ ...message, sentAt: new Date(message.sentAt) })),
  };
}
