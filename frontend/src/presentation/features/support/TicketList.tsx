import type { TicketSummary } from '@/domain/models/Support';
import { Icon } from '../../design-system';
import { useI18n } from '../../i18n/I18nContext';

interface TicketListProps {
  tickets: readonly TicketSummary[];
  selectedId: number | null;
  onSelect: (id: number) => void;
  /** Vue d'équipe : la ligne porte alors le pseudonyme du demandeur. */
  showAsker?: boolean;
}

/**
 * Liste de demandes.
 * <p>
 * Le statut est doublé d'un mot et d'une icône, jamais porté par la seule
 * couleur : une file d'attente se lit aussi sans distinguer le rouge du vert.
 */
export function TicketList({ tickets, selectedId, onSelect, showAsker = false }: TicketListProps) {
  const { t, formatDate } = useI18n();

  return (
    <ul className="tickets">
      {tickets.map((ticket) => (
        <li key={ticket.id}>
          <button
            type="button"
            className={['ticket', ticket.id === selectedId && 'ticket--active'].filter(Boolean).join(' ')}
            onClick={() => onSelect(ticket.id)}
            aria-current={ticket.id === selectedId}
          >
            <span className="ticket__head">
              <span className={`ticket__status ticket__status--${ticket.status.toLowerCase()}`}>
                <Icon name={ticket.status === 'RESOLVED' ? 'check' : ticket.lastFromStaff ? 'mail' : 'alert'} size={14} />
                {t(`support.status.${ticket.status}`)}
              </span>
              <span className="ticket__category">{t(`support.category.${ticket.category}`)}</span>
            </span>
            <span className="ticket__subject">{ticket.subject}</span>
            {ticket.lastMessage && <span className="ticket__excerpt">{ticket.lastMessage}</span>}
            <span className="ticket__meta">
              {showAsker && <span>{t('support.asked', { handle: ticket.handle })}</span>}
              <span>{t('support.messages', { count: ticket.messages })}</span>
              <span>{formatDate(ticket.updatedAt)}</span>
            </span>
          </button>
        </li>
      ))}
    </ul>
  );
}
