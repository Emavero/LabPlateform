import { Link } from 'react-router-dom';
import { displayNameOf } from '@/domain/models/User';
import { earnedCount, type ActivityEntry } from '@/domain/models/Profile';
import { Alert, Button, Icon, Panel, Spinner } from '../design-system';
import { ProgressPanel } from '../features/box/ProgressPanel';
import { TrackProgress } from '../features/course/TrackProgress';
import { useProfile } from '../hooks/useProfile';
import { useAuth } from '../state/AuthContext';

/** Profil du joueur : progression, hauts faits, filières et activité récente. */
export function ProfilePage() {
  const { user } = useAuth();
  const profile = useProfile();

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="page__eyebrow">Profil</p>
          <h1 className="page__title">{user ? displayNameOf(user) : 'Profil'}</h1>
          <p className="page__lead">
            {profile.loading
              ? 'Chargement de votre parcours…'
              : `${profile.progress.rankName} · ${profile.progress.points} points · ${earnedCount(
                  profile.achievements,
                )} haut${earnedCount(profile.achievements) > 1 ? 's' : ''} fait${
                  earnedCount(profile.achievements) > 1 ? 's' : ''
                } sur ${profile.achievements.length}`}
          </p>
        </div>
        <Button
          variant="ghost"
          size="sm"
          icon="refresh"
          loading={profile.loading}
          onClick={() => void profile.reload()}
        >
          Actualiser
        </Button>
      </header>

      {profile.error && (
        <Alert
          tone="error"
          title="Impossible de charger le profil"
          action={
            <Button variant="ghost" size="sm" icon="refresh" onClick={() => void profile.reload()}>
              Réessayer
            </Button>
          }
        >
          {profile.error.message}
        </Alert>
      )}

      <Panel
        title="Machines"
        actions={
          <Link className="btn btn--ghost btn--sm" to="/scoreboard">
            <Icon name="trophy" size={16} />
            <span>Classement</span>
          </Link>
        }
      >
        <ProgressPanel progress={profile.progress} />
      </Panel>

      <Panel title="Hauts faits" description="Ceux qui manquent indiquent quoi viser ensuite.">
        {profile.loading && profile.achievements.length === 0 ? (
          <div className="empty">
            <Spinner size={22} label="Chargement des hauts faits" />
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
          title={`Cours · ${track.trackName}`}
          actions={
            <Link className="btn btn--ghost btn--sm" to={`/cours/${track.trackSlug}`}>
              <Icon name="book" size={16} />
              <span>Ouvrir</span>
            </Link>
          }
        >
          <TrackProgress progress={track} />
        </Panel>
      ))}

      <Panel title="Activité récente">
        {profile.loading && profile.activity.length === 0 ? (
          <div className="empty">
            <Spinner size={22} label="Chargement de l'activité" />
          </div>
        ) : profile.activity.length === 0 ? (
          <p className="empty">Rien pour l'instant : validez un flag ou terminez une section de cours.</p>
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
              <Icon name="crown" size={12} /> First blood
            </span>
          )}
        </span>
        <span className="activity__detail">{entry.detail}</span>
      </span>
      {entry.points > 0 && <span className="activity__points">+{entry.points}</span>}
      <time className="activity__date" dateTime={entry.at.toISOString()}>
        {entry.at.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' })}
      </time>
    </li>
  );
}
