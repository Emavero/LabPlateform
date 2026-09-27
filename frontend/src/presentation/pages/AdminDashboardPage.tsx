import { Link } from 'react-router-dom';
import { filledInsights, WINDOWS } from '@/domain/models/Analytics';
import { formatMoney } from '@/domain/models/Billing';
import { Alert, Button, Icon, Panel, Spinner } from '../design-system';
import { RankingBars } from '../features/analytics/RankingBars';
import { RecommendationList } from '../features/analytics/RecommendationList';
import { SegmentBars } from '../features/analytics/SegmentBars';
import { StatTile } from '../features/analytics/StatTile';
import { JournalTimeline } from '../features/journal/JournalTimeline';
import { useAnalytics } from '../hooks/useAnalytics';
import { useJournal } from '../hooks/useJournal';

/**
 * Tableau de bord de l'administration.
 * <p>
 * Il répond dans cet ordre : combien de monde, ce qu'ils font, ce que ça
 * rapporte, qui ils sont, ce qui attire ou bloque, et quoi faire. Tous les
 * chiffres portent sur la fenêtre choisie — jamais des totaux depuis l'origine,
 * qui flatteraient une plateforme dont personne ne se sert plus.
 */
export function AdminDashboardPage() {
  const { analytics, windowDays, loading, error, setWindow, reload } = useAnalytics();
  const journal = useJournal('platform', 40);

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="page__eyebrow">Administration</p>
          <h1 className="page__title">Tableau de bord</h1>
          <p className="page__lead">
            Ce que les comptes font de la plateforme, sur {windowDays} jours, et ce qu'il y a à en tirer.
          </p>
        </div>
        <div className="page__actions">
          <div className="window-picker" role="group" aria-label="Fenêtre d'observation">
            {WINDOWS.map((days) => (
              <button
                key={days}
                type="button"
                className={['window-picker__option', windowDays === days && 'window-picker__option--active']
                  .filter(Boolean)
                  .join(' ')}
                aria-pressed={windowDays === days}
                onClick={() => setWindow(days)}
              >
                {days} j
              </button>
            ))}
          </div>
          <Button variant="ghost" size="sm" icon="refresh" loading={loading} onClick={() => void reload()}>
            Actualiser
          </Button>
        </div>
      </header>

      {error && (
        <Alert
          tone="error"
          title="Impossible de charger les indicateurs"
          action={
            <Button variant="ghost" size="sm" icon="refresh" onClick={() => void reload()}>
              Réessayer
            </Button>
          }
        >
          {error.message}
        </Alert>
      )}

      {loading && !analytics ? (
        <div className="empty">
          <Spinner size={22} label="Calcul des indicateurs" />
        </div>
      ) : (
        analytics && (
          <>
            <Panel
              title="Ce qu'il y a à faire"
              description="Déduit de ce que les comptes ont réellement fait, avec le chiffre qui le motive."
            >
              <RecommendationList recommendations={analytics.recommendations} />
            </Panel>

            <section className="kpi-row" aria-label="Indicateurs d'audience">
              <StatTile
                label="Comptes"
                value={analytics.audience.users}
                hint={`${analytics.audience.newUsers} nouveaux sur la période`}
              />
              <StatTile
                label="Comptes actifs"
                value={analytics.audience.activeUsers}
                previous={analytics.previous.activeUsers}
                hint={`${analytics.audience.activeRate} % de l'ensemble`}
              />
              <StatTile
                label="Comptes dormants"
                value={analytics.audience.dormantUsers}
                hint="Rien fait depuis l'inscription"
                upIsGood={false}
              />
              {/* Pas de variation ici : le nombre d'abonnés est un stock, et le
                  comparer aux abonnements ouverts la période d'avant — un flux —
                  donnerait un pourcentage qui ne veut rien dire. */}
              <StatTile
                label="Abonnés Pro"
                value={analytics.audience.proUsers}
                hint={`${analytics.audience.conversionRate} % de conversion`}
              />
              <StatTile
                label="Abonnements ouverts"
                value={analytics.revenue.paymentsSucceeded}
                previous={analytics.previous.subscriptionsStarted}
                hint={`${analytics.revenue.checkoutsStarted} paiements engagés`}
              />
              {/* Sans cohorte antérieure, la rétention n'est pas « nulle » :
                  il n'y a rien à mesurer, et 0 % se lirait comme un résultat. */}
              <StatTile
                label="Rétention"
                value={analytics.audience.retentionCohort > 0 ? analytics.audience.retentionRate : null}
                unit=" %"
                hint={
                  analytics.audience.retentionCohort > 0
                    ? `Sur ${analytics.audience.retentionCohort} comptes déjà là au début de la période`
                    : 'Aucun compte antérieur à la période : rien à mesurer.'
                }
              />
            </section>

            <section className="kpi-row" aria-label="Indicateurs d'usage">
              <StatTile
                label="Flags validés"
                value={analytics.engagement.flagsValidated}
                previous={analytics.previous.flagsValidated}
              />
              <StatTile
                label="Machines possédées"
                value={analytics.engagement.boxesPwned}
                hint={`${analytics.engagement.targetsSpawned} cibles lancées`}
              />
              <StatTile
                label="Flags refusés"
                value={
                  analytics.engagement.flagsValidated + analytics.engagement.flagsRefused > 0
                    ? analytics.engagement.flagRefusalRate
                    : null
                }
                unit=" %"
                hint={`${analytics.engagement.flagsRefused} soumissions refusées`}
                upIsGood={false}
              />
              <StatTile
                label="Sections terminées"
                value={analytics.engagement.sectionsCompleted}
                previous={analytics.previous.sectionsCompleted}
              />
              <StatTile
                label="Quiz réussis"
                value={
                  analytics.engagement.quizPassed + analytics.engagement.quizFailed > 0
                    ? analytics.engagement.quizPassRate
                    : null
                }
                unit=" %"
                hint={
                  analytics.engagement.quizPassed + analytics.engagement.quizFailed > 0
                    ? `${analytics.engagement.quizPassed} réussis, ${analytics.engagement.quizFailed} manqués`
                    : 'Aucun quiz passé sur la période.'
                }
              />
              <StatTile
                label="Comptes rendus"
                value={analytics.engagement.writeupsPublished}
                hint="Publiés sur la période"
              />
            </section>

            <div className="page__grid">
              <Panel
                title="Classes d'utilisateurs"
                description="Chaque compte est rangé selon la famille où il agit le plus."
              >
                <SegmentBars segments={analytics.segments} />
              </Panel>

              <Panel
                title="Encaissements"
                description="Un total par devise : des francs CFA et des euros ne s'additionnent pas."
              >
                {analytics.revenue.collected.length === 0 ? (
                  <p className="empty">Aucun paiement encaissé sur la période.</p>
                ) : (
                  <ul className="revenue">
                    {analytics.revenue.collected.map((money) => (
                      <li key={money.currency} className="revenue__row">
                        <span className="revenue__amount">{formatMoney(money)}</span>
                        <span className="revenue__currency">{money.currency}</span>
                      </li>
                    ))}
                  </ul>
                )}
                <dl className="revenue__facts">
                  <div>
                    <dt>Paiements aboutis</dt>
                    <dd>
                      {analytics.revenue.paymentsSucceeded} / {analytics.revenue.checkoutsStarted} engagés
                    </dd>
                  </div>
                  <div>
                    <dt>Refusés</dt>
                    <dd>
                      {analytics.revenue.paymentsFailed} ({analytics.revenue.failureRate} %)
                    </dd>
                  </div>
                  <div>
                    <dt>Parcours mené à terme</dt>
                    <dd>{analytics.revenue.checkoutCompletionRate} %</dd>
                  </div>
                </dl>
              </Panel>
            </div>

            <div className="page__grid">
              {filledInsights(analytics).map((insight) => (
                <Panel key={insight.code} title={insight.title}>
                  <RankingBars insight={insight} />
                </Panel>
              ))}
            </div>

            <Panel
              title="Contenu publié"
              actions={
                <>
                  <Link className="btn btn--ghost btn--sm" to="/admin/machines">
                    <Icon name="target" size={16} />
                    <span>Gérer les machines</span>
                  </Link>
                  <Link className="btn btn--primary btn--sm" to="/admin/cours">
                    <Icon name="book" size={16} />
                    <span>Gérer les cours</span>
                  </Link>
                </>
              }
            >
              <dl className="admin-stats">
                <div>
                  <dt>Machines</dt>
                  <dd>{analytics.catalogue.boxes ?? 0}</dd>
                </div>
                <div>
                  <dt>Cours</dt>
                  <dd>{analytics.catalogue.courses ?? 0}</dd>
                </div>
                <div>
                  <dt>Sections</dt>
                  <dd>{analytics.catalogue.sections ?? 0}</dd>
                </div>
              </dl>
            </Panel>

            <Panel
              title="Dernières actions"
              description="Journal de toute la plateforme. Les auteurs n'y figurent que par leur pseudonyme."
            >
              {journal.error ? (
                <Alert tone="error">{journal.error.message}</Alert>
              ) : journal.loading && journal.lines.length === 0 ? (
                <Spinner label="Chargement du journal…" />
              ) : (
                <JournalTimeline lines={journal.lines} showAuthor />
              )}
            </Panel>
          </>
        )
      )}
    </div>
  );
}
