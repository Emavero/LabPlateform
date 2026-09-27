import { useState, type FormEvent } from 'react';
import { EMPTY_TICKET_DRAFT, TICKET_CATEGORIES, type TicketCategory, type TicketDraft } from '@/domain/models/Support';
import { Alert, Button, Panel, Spinner, TextField } from '../design-system';
import { TicketList } from '../features/support/TicketList';
import { TicketThread } from '../features/support/TicketThread';
import { useI18n } from '../i18n/I18nContext';
import { useMyTickets } from '../hooks/useSupport';

/**
 * Assistance vue du demandeur : ses demandes, et le formulaire pour en ouvrir
 * une. Le fil remplace le formulaire dès qu'une demande est ouverte — deux
 * zones de saisie côte à côte inviteraient à écrire au mauvais endroit.
 */
export function SupportPage() {
  const { t } = useI18n();
  const tickets = useMyTickets();
  const [draft, setDraft] = useState<TicketDraft>(EMPTY_TICKET_DRAFT);

  const patch = (changes: Partial<TicketDraft>) => setDraft((current) => ({ ...current, ...changes }));
  const fieldErrors = tickets.error?.fieldErrors ?? {};

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (await tickets.open(draft)) setDraft(EMPTY_TICKET_DRAFT);
  };

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="page__eyebrow">{t('support.title')}</p>
          <h1 className="page__title">{t('support.title')}</h1>
          <p className="page__lead">{t('support.lead')}</p>
        </div>
        <Button variant="ghost" size="sm" icon="refresh" loading={tickets.loading} onClick={() => void tickets.reload()}>
          {t('common.refresh')}
        </Button>
      </header>

      {tickets.error && Object.keys(fieldErrors).length === 0 && (
        <Alert tone="error" title={t('support.loadError')}>
          {tickets.error.message}
        </Alert>
      )}

      <div className="page__grid">
        <Panel title={t('support.mine')}>
          {tickets.loading && tickets.tickets.length === 0 ? (
            <div className="empty">
              <Spinner size={22} label={t('support.loading')} />
            </div>
          ) : tickets.tickets.length === 0 ? (
            <p className="empty">{t('support.empty')}</p>
          ) : (
            <TicketList
              tickets={tickets.tickets}
              selectedId={tickets.opened?.id ?? null}
              onSelect={(id) => void tickets.select(id)}
            />
          )}
        </Panel>

        {tickets.opened ? (
          <div className="support-detail">
            <Button variant="ghost" size="sm" icon="chevronRight" onClick={() => void tickets.select(null)}>
              {t('support.new')}
            </Button>
            <TicketThread
              ticket={tickets.opened}
              busy={tickets.busy}
              onReply={tickets.reply}
              onResolve={tickets.resolve}
            />
          </div>
        ) : (
          <Panel title={t('support.new')} description={t('support.newHint')}>
            <form className="form" onSubmit={submit} noValidate>
              <label className="field">
                <span className="field__label">{t('support.category')}</span>
                <select
                  className="field__input"
                  value={draft.category}
                  onChange={(event) => patch({ category: event.target.value as TicketCategory })}
                >
                  {TICKET_CATEGORIES.map((category) => (
                    <option key={category} value={category}>
                      {t(`support.category.${category}`)}
                    </option>
                  ))}
                </select>
              </label>
              <TextField
                label={t('support.subject')}
                value={draft.subject}
                onChange={(event) => patch({ subject: event.target.value })}
                error={fieldErrors.subject}
                placeholder={t('support.subjectPlaceholder')}
              />
              <label className="field">
                <span className="field__label">{t('support.message')}</span>
                <textarea
                  className="field__input editor-textarea"
                  rows={8}
                  value={draft.body}
                  onChange={(event) => patch({ body: event.target.value })}
                  placeholder={t('support.messagePlaceholder')}
                />
                {fieldErrors.body && <span className="field__error">{fieldErrors.body}</span>}
              </label>
              <Button type="submit" icon="mail" loading={tickets.busy} loadingLabel={t('support.sending')}>
                {t('support.send')}
              </Button>
            </form>
          </Panel>
        )}
      </div>
    </div>
  );
}
