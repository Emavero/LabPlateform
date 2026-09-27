import { useState, type FormEvent } from 'react';
import type { Ticket } from '@/domain/models/Support';
import { Alert, Button, Icon, Panel } from '../../design-system';
import { useI18n } from '../../i18n/I18nContext';

interface TicketThreadProps {
  ticket: Ticket;
  busy: boolean;
  onReply: (body: string) => Promise<boolean>;
  onResolve: () => Promise<void>;
  /** Lecture par l'équipe : les messages du demandeur sont alors « en face ». */
  asStaff?: boolean;
}

/**
 * Fil d'une demande, du plus ancien message au plus récent.
 * <p>
 * Le côté d'un message vient de {@code fromStaff}, jamais du compte qui lit :
 * un administrateur relisant sa propre demande doit y voir ses messages là où
 * son demandeur les voit.
 */
export function TicketThread({ ticket, busy, onReply, onResolve, asStaff = false }: TicketThreadProps) {
  const { t, formatDateTime } = useI18n();
  const [body, setBody] = useState('');

  const send = async (event: FormEvent) => {
    event.preventDefault();
    if (await onReply(body)) setBody('');
  };

  const mineSide = (fromStaff: boolean) => (asStaff ? fromStaff : !fromStaff);

  return (
    <Panel
      title={ticket.subject}
      description={t('support.openedOn', { date: formatDateTime(ticket.createdAt) })}
      actions={
        ticket.status === 'RESOLVED' ? undefined : (
          <Button variant="ghost" size="sm" icon="check" loading={busy} onClick={() => void onResolve()}>
            {t('support.resolve')}
          </Button>
        )
      }
    >
      <p className="thread__state">
        <span className={`ticket__status ticket__status--${ticket.status.toLowerCase()}`}>
          <Icon name={ticket.status === 'RESOLVED' ? 'check' : 'alert'} size={14} />
          {t(`support.status.${ticket.status}`)}
        </span>
        <span>{t(`support.category.${ticket.category}`)}</span>
      </p>

      <ol className="thread">
        {ticket.messages.map((message, index) => (
          <li
            key={index}
            className={['thread__item', mineSide(message.fromStaff) && 'thread__item--mine'].filter(Boolean).join(' ')}
          >
            <p className="thread__author">
              <Icon name={message.fromStaff ? 'shield' : 'user'} size={14} />
              {message.fromStaff ? t('support.fromStaff') : asStaff ? t('support.fromAsker') : t('support.you')}
              <time dateTime={message.sentAt.toISOString()}>{formatDateTime(message.sentAt)}</time>
            </p>
            <p className="thread__body">{message.body}</p>
          </li>
        ))}
      </ol>

      {ticket.status === 'RESOLVED' && <Alert tone="info">{t('support.resolved')}</Alert>}

      <form className="thread__reply" onSubmit={send}>
        <label className="field">
          <span className="field__label">{t('support.reply')}</span>
          <textarea
            className="field__input editor-textarea"
            rows={4}
            value={body}
            onChange={(event) => setBody(event.target.value)}
            placeholder={t('support.replyPlaceholder')}
          />
        </label>
        <Button type="submit" icon="mail" loading={busy} loadingLabel={t('support.sending')} disabled={!body.trim()}>
          {t('support.reply')}
        </Button>
      </form>
    </Panel>
  );
}
