import { Alert, Button, Panel, Spinner } from '../design-system';
import { JournalTimeline } from '../features/journal/JournalTimeline';
import { useJournal } from '../hooks/useJournal';

/**
 * Module Events : le journal de ses propres actions.
 * <p>
 * Il ne contient que des actes faits côté serveur — aucun suivi de navigation :
 * ce que la plateforme sait de vous est exactement ce que vous lui avez demandé.
 */
export function EventsPage() {
  const journal = useJournal('mine', 150);

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="page__eyebrow">Events</p>
          <h1 className="page__title">Journal d’activité</h1>
          <p className="page__lead">
            Vos actions sur la plateforme, du plus récent au plus ancien. Aucun suivi de navigation : seules les
            actions demandées au serveur y figurent.
          </p>
        </div>
        <Button variant="ghost" size="sm" icon="refresh" loading={journal.loading} onClick={() => void journal.reload()}>
          Actualiser
        </Button>
      </header>

      {journal.error && (
        <Alert tone="error" title="Journal indisponible">
          {journal.error.message}
        </Alert>
      )}

      <Panel title="Historique" description={`${journal.lines.length} événements`}>
        {journal.loading && journal.lines.length === 0 ? (
          <Spinner label="Chargement du journal…" />
        ) : (
          <JournalTimeline lines={journal.lines} />
        )}
      </Panel>
    </div>
  );
}
