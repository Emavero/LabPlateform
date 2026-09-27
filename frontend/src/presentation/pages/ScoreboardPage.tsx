import { Alert, Button, Icon, Panel, Spinner } from '../design-system';
import { ProgressPanel } from '../features/box/ProgressPanel';
import { useI18n } from '../i18n/I18nContext';
import { useScoreboard } from '../hooks/useScoreboard';

/** Classement public et progression personnelle. */
export function ScoreboardPage() {
  const { t } = useI18n();
  const scoreboard = useScoreboard();

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <h1 className="page__title">{t('scoreboard.title')}</h1>
          <p className="page__lead">{t('scoreboard.lead')}</p>
        </div>
        <Button
          variant="ghost"
          size="sm"
          icon="refresh"
          loading={scoreboard.loading}
          onClick={() => void scoreboard.reload()}
        >
          {t('common.refresh')}
        </Button>
      </header>

      <Panel title={t('machines.progress')}>
        <ProgressPanel progress={scoreboard.progress} />
      </Panel>

      <Panel title={t('scoreboard.best')} description={t('scoreboard.bestHint')}>
        {scoreboard.loading && scoreboard.entries.length === 0 ? (
          <div className="empty">
            <Spinner size={22} label={t('scoreboard.loading')} />
          </div>
        ) : scoreboard.error ? (
          <Alert
            tone="error"
            title={t('scoreboard.loadError')}
            action={
              <Button variant="ghost" size="sm" icon="refresh" onClick={() => void scoreboard.reload()}>
                {t('common.retry')}
              </Button>
            }
          >
            {scoreboard.error.message}
          </Alert>
        ) : scoreboard.entries.length === 0 ? (
          <p className="empty">{t('scoreboard.empty')}</p>
        ) : (
          <table className="leaderboard">
            <thead>
              <tr>
                <th scope="col">#</th>
                <th scope="col">{t('scoreboard.player')}</th>
                <th scope="col">{t('scoreboard.rank')}</th>
                <th scope="col">{t('scoreboard.flags')}</th>
                <th scope="col">{t('scoreboard.firstBloods')}</th>
                <th scope="col">{t('scoreboard.points')}</th>
              </tr>
            </thead>
            <tbody>
              {scoreboard.entries.map((entry) => (
                <tr key={entry.position} className={entry.self ? 'leaderboard__row--self' : undefined}>
                  <td className="leaderboard__position">{entry.position}</td>
                  <td>
                    {entry.handle}
                    {entry.self && <span className="leaderboard__you">{t('scoreboard.you')}</span>}
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
