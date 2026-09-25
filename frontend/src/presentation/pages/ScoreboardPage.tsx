import { Alert, Button, Icon, Panel, Spinner } from '../design-system';
import { ProgressPanel } from '../features/box/ProgressPanel';
import { useScoreboard } from '../hooks/useScoreboard';

/** Classement public et progression personnelle. */
export function ScoreboardPage() {
  const scoreboard = useScoreboard();

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <h1 className="page__title">Classement</h1>
          <p className="page__lead">
            Les joueurs sont classés aux points. À égalité, celui qui y est arrivé le premier passe devant.
          </p>
        </div>
        <Button
          variant="ghost"
          size="sm"
          icon="refresh"
          loading={scoreboard.loading}
          onClick={() => void scoreboard.reload()}
        >
          Actualiser
        </Button>
      </header>

      <Panel title="Votre progression">
        <ProgressPanel progress={scoreboard.progress} />
      </Panel>

      <Panel title="Meilleurs joueurs" description="Pseudonymes dérivés du compte : aucune adresse e-mail n'est publiée.">
        {scoreboard.loading && scoreboard.entries.length === 0 ? (
          <div className="empty">
            <Spinner size={22} label="Chargement du classement" />
          </div>
        ) : scoreboard.error ? (
          <Alert
            tone="error"
            title="Impossible de charger le classement"
            action={
              <Button variant="ghost" size="sm" icon="refresh" onClick={() => void scoreboard.reload()}>
                Réessayer
              </Button>
            }
          >
            {scoreboard.error.message}
          </Alert>
        ) : scoreboard.entries.length === 0 ? (
          <p className="empty">Personne n'a encore validé de flag. La première place est à prendre.</p>
        ) : (
          <table className="leaderboard">
            <thead>
              <tr>
                <th scope="col">#</th>
                <th scope="col">Joueur</th>
                <th scope="col">Rang</th>
                <th scope="col">Flags</th>
                <th scope="col">First bloods</th>
                <th scope="col">Points</th>
              </tr>
            </thead>
            <tbody>
              {scoreboard.entries.map((entry) => (
                <tr key={entry.position} className={entry.self ? 'leaderboard__row--self' : undefined}>
                  <td className="leaderboard__position">{entry.position}</td>
                  <td>
                    {entry.handle}
                    {entry.self && <span className="leaderboard__you">vous</span>}
                  </td>
                  <td>{entry.rankName}</td>
                  <td>{entry.ownedFlags}</td>
                  <td>
                    {entry.firstBloods > 0 ? (
                      <span className="leaderboard__blood">
                        <Icon name="crown" size={13} /> {entry.firstBloods}
                      </span>
                    ) : (
                      '—'
                    )}
                  </td>
                  <td className="leaderboard__points">{entry.points}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Panel>
    </div>
  );
}
