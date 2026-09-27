import { useState } from 'react';
import { queueSize, type TicketSummary } from '@/domain/models/Support';
import { Alert, Button, Panel, Spinner } from '../design-system';
import { StatTile } from '../features/analytics/StatTile';
import { TicketList } from '../features/support/TicketList';
import { TicketThread } from '../features/support/TicketThread';
import { useI18n } from '../i18n/I18nContext';
import { useSupportQueue } from '../hooks/useSupport';

type Tab = 'waiting' | 'answered' | 'resolved';

/**
 * File d'assistance de l'administration.
 * <p>
 * Trois onglets, dans l'ordre de ce qu'ils réclament : à traiter, en attente du
 * demandeur, résolues. L'onglet d'arrivée est celui qui attend l'équipe, pour
 * que la page ouvre sur le travail à faire.
 */
export function AdminSupportPage() {
  const { t } = useI18n();
  const support = useSupportQueue();
  const [tab, setTab] = useState<Tab>('waiting');

  const lists: Record<Tab, readonly TicketSummary[]> = {
    waiting: support.queue?.waiting ?? [],
    answered: support.queue?.answered ?? [],
    resolved: support.queue?.resolved ?? [],
  };
  const labels: Record<Tab, 'support.waiting' | 'support.answered' | 'support.closed'> = {
    waiting: 'support.waiting',
    answered: 'support.answered',
    resolved: 'support.closed',
  };

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="page__eyebrow">{t('admin.eyebrow')}</p>
          <h1 className="page__title">{t('support.queue')}</h1>
          <p className="page__lead">{t('support.queueLead')}</p>
        </div>
        <Button variant="ghost" size="sm" icon="refresh" loading={support.loading} onClick={() => void support.reload()}>
          {t('common.refresh')}
        </Button>
      </header>

      {support.error && (
        <Alert tone="error" title={t('support.loadError')}>
          {support.error.message}
        </Alert>
      )}

      {support.loading && !support.queue ? (
        <div className="empty">
          <Spinner size={22} label={t('support.loading')} />
        </div>
      ) : (
        support.queue && (
          <>
            <div className="kpi-row">
              <StatTile label={t('support.waiting')} value={support.queue.waiting.length} />
              <StatTile label={t('support.answered')} value={support.queue.answered.length} />
              <StatTile label={t('support.closed')} value={support.queue.resolved.length} />
              {/* Sans aucune réponse, la tuile affiche un tiret : le délai n'est pas nul, il n'existe pas. */}
              <StatTile
                label={t('support.median')}
                value={support.queue.medianMinutesToFirstReply}
                unit={t('support.minutesUnit')}
              />
            </div>

            <div className="page__grid">
              <Panel
                title={t('support.queue')}
                description={t('support.ticketCount', { count: queueSize(support.queue) })}
              >
                <div className="tabs" role="tablist" aria-label={t('support.queue')}>
                  {(['waiting', 'answered', 'resolved'] as const).map((key) => (
                    <button
                      key={key}
                      type="button"
                      role="tab"
                      aria-selected={tab === key}
                      className={['tabs__tab', tab === key && 'tabs__tab--active'].filter(Boolean).join(' ')}
                      onClick={() => setTab(key)}
                    >
                      {t(labels[key])} ({lists[key].length})
                    </button>
                  ))}
                </div>
                {lists[tab].length === 0 ? (
                  <p className="empty">{t(tab === 'waiting' ? 'support.noneWaiting' : 'common.empty')}</p>
                ) : (
                  <TicketList
                    tickets={lists[tab]}
                    selectedId={support.opened?.id ?? null}
                    onSelect={(id) => void support.select(id)}
                    showAsker
                  />
                )}
              </Panel>

              {support.opened ? (
                <TicketThread
                  ticket={support.opened}
                  busy={support.busy}
                  onReply={support.reply}
                  onResolve={support.resolve}
                  asStaff
                />
              ) : (
                <Panel title={t('support.byCategory')}>
                  {support.queue.byCategory.length === 0 ? (
                    <p className="empty">{t('common.empty')}</p>
                  ) : (
                    <ul className="tally">
                      {support.queue.byCategory.map((entry) => (
                        <li key={entry.category} className="tally__row">
                          <span className="tally__label">{t(`support.category.${entry.category}`)}</span>
                          <span className="tally__count">{entry.count}</span>
                        </li>
                      ))}
                    </ul>
                  )}
                  <p className="empty">{t('support.pick')}</p>
                </Panel>
              )}
            </div>
          </>
        )
      )}
    </div>
  );
}
