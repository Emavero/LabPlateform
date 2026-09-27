import { Alert, Button, Panel, Spinner } from '../design-system';
import { JournalTimeline } from '../features/journal/JournalTimeline';
import { useI18n } from '../i18n/I18nContext';
import { useJournal } from '../hooks/useJournal';

/**
 * Module Events : le journal de ses propres actions.
 * <p>
 * Il ne contient que des actes faits côté serveur — aucun suivi de navigation :
 * ce que la plateforme sait de vous est exactement ce que vous lui avez demandé.
 */
export function EventsPage() {
  const { t } = useI18n();
  const journal = useJournal('mine', 150);

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="page__eyebrow">{t('journal.eyebrow')}</p>
          <h1 className="page__title">{t('journal.title')}</h1>
          <p className="page__lead">{t('journal.lead')}</p>
        </div>
        <Button variant="ghost" size="sm" icon="refresh" loading={journal.loading} onClick={() => void journal.reload()}>
          {t('common.refresh')}
        </Button>
      </header>

      {journal.error && (
        <Alert tone="error" title={t('journal.unavailable')}>
          {journal.error.message}
        </Alert>
      )}

      <Panel title={t('journal.history')} description={t('journal.count', { count: journal.lines.length })}>
        {journal.loading && journal.lines.length === 0 ? (
          <Spinner label={t('common.loading')} />
        ) : (
          <JournalTimeline lines={journal.lines} />
        )}
      </Panel>
    </div>
  );
}
