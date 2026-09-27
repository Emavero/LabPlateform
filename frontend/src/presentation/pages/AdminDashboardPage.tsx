import { Link } from 'react-router-dom';
import { filledInsights, WINDOWS } from '@/domain/models/Analytics';
import { formatMoney } from '@/domain/models/Billing';
import { Alert, Button, Icon, Panel, Spinner } from '../design-system';
import { RankingBars } from '../features/analytics/RankingBars';
import { RecommendationList } from '../features/analytics/RecommendationList';
import { SegmentBars } from '../features/analytics/SegmentBars';
import { StatTile } from '../features/analytics/StatTile';
import { JournalTimeline } from '../features/journal/JournalTimeline';
import { useI18n } from '../i18n/I18nContext';
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
  const { t, locale } = useI18n();
  const { analytics, windowDays, loading, error, setWindow, reload } = useAnalytics();
  const journal = useJournal('platform', 40);

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="page__eyebrow">{t('admin.eyebrow')}</p>
          <h1 className="page__title">{t('admin.title')}</h1>
          <p className="page__lead">{t('admin.lead', { days: windowDays })}</p>
        </div>
        <div className="page__actions">
          <div className="window-picker" role="group" aria-label={t('admin.window')}>
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
                {t('admin.windowDays', { days })}
              </button>
            ))}
          </div>
          <Button variant="ghost" size="sm" icon="refresh" loading={loading} onClick={() => void reload()}>
            {t('common.refresh')}
          </Button>
        </div>
      </header>

      {error && (
        <Alert
          tone="error"
          title={t('admin.metricsUnavailable')}
          action={
            <Button variant="ghost" size="sm" icon="refresh" onClick={() => void reload()}>
              {t('common.retry')}
            </Button>
          }
        >
          {error.message}
        </Alert>
      )}

      {loading && !analytics ? (
        <div className="empty">
          <Spinner size={22} label={t('admin.computing')} />
        </div>
      ) : (
        analytics && (
          <>
            <Panel
              title={t('admin.todo')}
              description={t('admin.todoHint')}
            >
              <RecommendationList recommendations={analytics.recommendations} />
            </Panel>

            <section className="kpi-row" aria-label={t('admin.audienceGroup')}>
              <StatTile
                label={t('admin.accounts')}
                value={analytics.audience.users}
                hint={t('admin.accountsHint', { count: analytics.audience.newUsers })}
              />
              <StatTile
                label={t('admin.activeAccounts')}
                value={analytics.audience.activeUsers}
                previous={analytics.previous.activeUsers}
                hint={t('admin.activeHint', { rate: analytics.audience.activeRate })}
              />
              <StatTile
                label={t('admin.dormantAccounts')}
                value={analytics.audience.dormantUsers}
                hint={t('admin.dormantHint')}
                upIsGood={false}
              />
              {/* Pas de variation ici : le nombre d'abonnés est un stock, et le
                  comparer aux abonnements ouverts la période d'avant — un flux —
                  donnerait un pourcentage qui ne veut rien dire. */}
              <StatTile
                label={t('admin.proAccounts')}
                value={analytics.audience.proUsers}
                hint={t('admin.proHint', { rate: analytics.audience.conversionRate })}
              />
              <StatTile
                label={t('admin.subscriptionsOpened')}
                value={analytics.revenue.paymentsSucceeded}
                previous={analytics.previous.subscriptionsStarted}
                hint={t('admin.subscriptionsHint', { count: analytics.revenue.checkoutsStarted })}
              />
              {/* Sans cohorte antérieure, la rétention n'est pas « nulle » :
                  il n'y a rien à mesurer, et 0 % se lirait comme un résultat. */}
              <StatTile
                label={t('admin.retention')}
                value={analytics.audience.retentionCohort > 0 ? analytics.audience.retentionRate : null}
                unit=" %"
                hint={
                  analytics.audience.retentionCohort > 0
                    ? t('admin.retentionHint', { count: analytics.audience.retentionCohort })
                    : t('admin.retentionNone')
                }
              />
            </section>

            <section className="kpi-row" aria-label={t('admin.usageGroup')}>
              <StatTile
                label={t('admin.flagsValidated')}
                value={analytics.engagement.flagsValidated}
                previous={analytics.previous.flagsValidated}
              />
              <StatTile
                label={t('admin.boxesPwned')}
                value={analytics.engagement.boxesPwned}
                hint={t('admin.boxesPwnedHint', { count: analytics.engagement.targetsSpawned })}
              />
              <StatTile
                label={t('admin.flagsRefused')}
                value={
                  analytics.engagement.flagsValidated + analytics.engagement.flagsRefused > 0
                    ? analytics.engagement.flagRefusalRate
                    : null
                }
                unit=" %"
                hint={t('admin.flagsRefusedHint', { count: analytics.engagement.flagsRefused })}
                upIsGood={false}
              />
              <StatTile
                label={t('admin.sectionsCompleted')}
                value={analytics.engagement.sectionsCompleted}
                previous={analytics.previous.sectionsCompleted}
              />
              <StatTile
                label={t('admin.quizPassed')}
                value={
                  analytics.engagement.quizPassed + analytics.engagement.quizFailed > 0
                    ? analytics.engagement.quizPassRate
                    : null
                }
                unit=" %"
                hint={
                  analytics.engagement.quizPassed + analytics.engagement.quizFailed > 0
                    ? t('admin.quizHint', {
                        passed: analytics.engagement.quizPassed,
                        failed: analytics.engagement.quizFailed,
                      })
                    : t('admin.quizNone')
                }
              />
              <StatTile
                label={t('admin.writeups')}
                value={analytics.engagement.writeupsPublished}
                hint={t('admin.writeupsHint')}
              />
            </section>

            <div className="page__grid">
              <Panel
                title={t('admin.segments')}
                description={t('admin.segmentsHint')}
              >
                <SegmentBars segments={analytics.segments} />
              </Panel>

              <Panel
                title={t('admin.revenue')}
                description={t('admin.revenueHint')}
              >
                {analytics.revenue.collected.length === 0 ? (
                  <p className="empty">{t('admin.revenueEmpty')}</p>
                ) : (
                  <ul className="revenue">
                    {analytics.revenue.collected.map((money) => (
                      <li key={money.currency} className="revenue__row">
                        <span className="revenue__amount">{formatMoney(money, locale)}</span>
                        <span className="revenue__currency">{money.currency}</span>
                      </li>
                    ))}
                  </ul>
                )}
                <dl className="revenue__facts">
                  <div>
                    <dt>{t('admin.paymentsSettled')}</dt>
                    <dd>
                      {t('admin.paymentsSettledValue', {
                        succeeded: analytics.revenue.paymentsSucceeded,
                        started: analytics.revenue.checkoutsStarted,
                      })}
                    </dd>
                  </div>
                  <div>
                    <dt>{t('admin.paymentsRefused')}</dt>
                    <dd>
                      {t('admin.paymentsRefusedValue', {
                        count: analytics.revenue.paymentsFailed,
                        rate: analytics.revenue.failureRate,
                      })}
                    </dd>
                  </div>
                  <div>
                    <dt>{t('admin.checkoutCompleted')}</dt>
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
              title={t('admin.published')}
              actions={
                <>
                  <Link className="btn btn--ghost btn--sm" to="/admin/machines">
                    <Icon name="target" size={16} />
                    <span>{t('nav.adminMachines')}</span>
                  </Link>
                  <Link className="btn btn--primary btn--sm" to="/admin/cours">
                    <Icon name="book" size={16} />
                    <span>{t('nav.adminCourses')}</span>
                  </Link>
                </>
              }
            >
              <dl className="admin-stats">
                <div>
                  <dt>{t('admin.machines')}</dt>
                  <dd>{analytics.catalogue.boxes ?? 0}</dd>
                </div>
                <div>
                  <dt>{t('admin.courses')}</dt>
                  <dd>{analytics.catalogue.courses ?? 0}</dd>
                </div>
                <div>
                  <dt>{t('admin.sections')}</dt>
                  <dd>{analytics.catalogue.sections ?? 0}</dd>
                </div>
              </dl>
            </Panel>

            <Panel
              title={t('admin.latest')}
              description={t('admin.latestHint')}
            >
              {journal.error ? (
                <Alert tone="error">{journal.error.message}</Alert>
              ) : journal.loading && journal.lines.length === 0 ? (
                <Spinner label={t('common.loading')} />
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
