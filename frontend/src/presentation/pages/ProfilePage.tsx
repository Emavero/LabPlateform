import { Link } from 'react-router-dom';
import { displayNameOf } from '@/domain/models/User';
import { earnedCount, type ActivityEntry } from '@/domain/models/Profile';
import { Alert, Button, Icon, Panel, Spinner } from '../design-system';
import { ProgressPanel } from '../features/box/ProgressPanel';
import { TrackProgress } from '../features/course/TrackProgress';
import { useI18n } from '../i18n/I18nContext';
import { useProfile } from '../hooks/useProfile';
import { useAuth } from '../state/AuthContext';

/** Profil du joueur : progression, hauts faits, filières et activité récente. */
export function ProfilePage() {
  const { t } = useI18n();
  const { user } = useAuth();
  const profile = useProfile();

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="page__eyebrow">{t('profile.eyebrow')}</p>
          <h1 className="page__title">{user ? displayNameOf(user) : t('profile.eyebrow')}</h1>
          <p className="page__lead">
            {profile.loading
              ? t('profile.loading')
              : t('profile.summary', {
                  rank: profile.progress.rankName,
                  points: profile.progress.points,
                  earned: earnedCount(profile.achievements),
                  total: profile.achievements.length,
                })}
          </p>
        </div>
        <Button
          variant="ghost"
          size="sm"
          icon="refresh"
          loading={profile.loading}
          onClick={() => void profile.reload()}
        >
          {t('common.refresh')}
        </Button>
      </header>

      {profile.error && (
        <Alert
          tone="error"
          title={t('profile.loadError')}
          action={
            <Button variant="ghost" size="sm" icon="refresh" onClick={() => void profile.reload()}>
              {t('common.retry')}
            </Button>
          }
        >
          {profile.error.message}
        </Alert>
      )}

      <Panel
        title={t('nav.machines')}
        actions={
          <Link className="btn btn--ghost btn--sm" to="/scoreboard">
            <Icon name="trophy" size={16} />
            <span>{t('nav.scoreboard')}</span>
          </Link>
        }
      >
        <ProgressPanel progress={profile.progress} />
      </Panel>

      <Panel title={t('profile.achievements')} description={t('profile.achievementsHint')}>
        {profile.loading && profile.achievements.length === 0 ? (
          <div className="empty">
            <Spinner size={22} label={t('profile.loadingAchievements')} />
          </div>
        ) : (
          <ul className="achievements">
            {profile.achievements.map((achievement) => (
              <li
                key={achievement.code}
                className={['achievement', achievement.earned && 'achievement--earned']
                  .filter(Boolean)
                  .join(' ')}
              >
                <span className="achievement__icon">
                  <Icon name={achievement.earned ? 'medal' : 'lock'} size={18} />
                </span>
                <span className="achievement__text">
                  <span className="achievement__name">{achievement.name}</span>
                  <span className="achievement__requirement">{achievement.requirement}</span>
                </span>
              </li>
            ))}
          </ul>
        )}
      </Panel>

      {profile.learning.map((track) => (
        <Panel
          key={track.trackSlug}
          title={t('profile.coursesOf', { track: track.trackName })}
          actions={
            <Link className="btn btn--ghost btn--sm" to={`/cours/${track.trackSlug}`}>
              <Icon name="book" size={16} />
              <span>{t('profile.open')}</span>
            </Link>
          }
        >
          <TrackProgress progress={track} />
        </Panel>
      ))}

      <Panel title={t('profile.activity')}>
        {profile.loading && profile.activity.length === 0 ? (
          <div className="empty">
            <Spinner size={22} label={t('profile.loadingActivity')} />
          </div>
        ) : profile.activity.length === 0 ? (
          <p className="empty">{t('profile.activityEmpty')}</p>
        ) : (
          <ol className="activity">
            {profile.activity.map((entry, index) => (
              <ActivityRow key={`${entry.at.toISOString()}-${index}`} entry={entry} />
            ))}
          </ol>
        )}
      </Panel>
    </div>
  );
}

function ActivityRow({ entry }: { entry: ActivityEntry }) {
  const { t, locale } = useI18n();
  return (
    <li className="activity__item">
      <span className={`activity__icon activity__icon--${entry.kind.toLowerCase()}`}>
        <Icon name={entry.kind === 'FLAG' ? 'flag' : 'book'} size={16} />
      </span>
      <span className="activity__text">
        <span className="activity__title">
          {entry.title}
          {entry.firstBlood && (
            <span className="badge badge--blood">
              <Icon name="crown" size={12} /> {t('boxes.firstBlood')}
            </span>
          )}
        </span>
        <span className="activity__detail">{entry.detail}</span>
      </span>
      {entry.points > 0 && <span className="activity__points">+{entry.points}</span>}
      <time className="activity__date" dateTime={entry.at.toISOString()}>
        {entry.at.toLocaleDateString(locale, {
          day: '2-digit',
          month: 'short',
          hour: '2-digit',
          minute: '2-digit',
        })}
      </time>
    </li>
  );
}
