import { useMemo } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import {
  filterCourses,
  topicsOf,
  topicStillValid,
  type CatalogueFilter,
} from '@/domain/models/CourseCatalogue';
import type { Track } from '@/domain/models/Course';
import { Alert, Button, Icon, Panel, Spinner } from '../design-system';
import { CourseCard } from '../features/course/CourseCard';
import { TrackProgress } from '../features/course/TrackProgress';
import { useI18n } from '../i18n/I18nContext';
import { useCatalogue } from '../hooks/useCatalogue';

/** Paramètre d'URL du sous-domaine : le filtre survit à un rechargement. */
const TOPIC_PARAM = 'domaine';

/** Icône associée à une filière, pour que le bouton se reconnaisse d'un coup d'œil. */
const TRACK_ICONS: Readonly<Record<string, 'search' | 'shield'>> = {
  FORENSICS: 'search',
  DEFENSE: 'shield',
};

/**
 * Page globale des cours.
 * <p>
 * La sélection vit dans l'URL, pas dans un état local : la filière dans le
 * chemin — ce qui laisse les liens de cours en `/cours/:filiere/:cours`
 * inchangés — et le sous-domaine en paramètre. Un filtre se partage donc, et le
 * bouton « précédent » du navigateur le défait.
 */
export function CoursesPage() {
  const { t } = useI18n();
  const navigate = useNavigate();
  const { track } = useParams();
  const [params, setParams] = useSearchParams();
  const catalogue = useCatalogue();

  const trackSlug = track ?? null;
  const requestedTopic = params.get(TOPIC_PARAM);
  // Un sous-domaine d'une autre filière est oublié plutôt que de vider la page
  // sans explication.
  const topicSlug = topicStillValid(catalogue.tracks, trackSlug, requestedTopic) ? requestedTopic : null;

  const filter = useMemo<CatalogueFilter>(() => ({ trackSlug, topicSlug }), [trackSlug, topicSlug]);
  const shown = useMemo(() => filterCourses(catalogue.courses, filter), [catalogue.courses, filter]);
  const topics = useMemo(
    () => topicsOf(catalogue.tracks, filterCourses(catalogue.courses, { trackSlug, topicSlug: null }), trackSlug),
    [catalogue.tracks, catalogue.courses, trackSlug],
  );

  const selectTrack = (candidate: Track) => {
    // Recliquer la filière active revient à tout afficher.
    navigate(candidate.slug === trackSlug ? '/cours' : `/cours/${candidate.slug}`);
  };

  const selectTopic = (slug: string | null) => {
    const next = new URLSearchParams(params);
    if (slug === null || slug === topicSlug) next.delete(TOPIC_PARAM);
    else next.set(TOPIC_PARAM, slug);
    setParams(next, { replace: true });
  };

  const activeTrack = catalogue.tracks.find((candidate) => candidate.slug === trackSlug);
  const lead = activeTrack ? activeTrack.description : t('courses.allLead');

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="page__eyebrow">{t('courses.eyebrow')}</p>
          <h1 className="page__title">{activeTrack ? activeTrack.name : t('courses.allTitle')}</h1>
          <p className="page__lead">{lead}</p>
        </div>
        <Button
          variant="ghost"
          size="sm"
          icon="refresh"
          loading={catalogue.loading}
          onClick={() => void catalogue.reload()}
        >
          {t('common.refresh')}
        </Button>
      </header>

      {/* Deux niveaux de filtre : la filière, puis ses sous-domaines. Le second
          n'apparaît qu'une fois le premier choisi — proposer les sous-domaines
          des deux filières à la fois ferait une vingtaine de boutons sans
          rapport entre eux. */}
      <section className="catalogue-filters" aria-label={t('courses.filters')}>
        <div className="catalogue-filters__tracks" role="group" aria-label={t('courses.tracksFilter')}>
          {catalogue.tracks.map((candidate) => {
            const active = candidate.slug === trackSlug;
            const count = catalogue.courses.filter((course) => course.trackSlug === candidate.slug).length;
            return (
              <button
                key={candidate.slug}
                type="button"
                className={['track-chip', active && 'track-chip--active'].filter(Boolean).join(' ')}
                aria-pressed={active}
                onClick={() => selectTrack(candidate)}
              >
                <Icon name={TRACK_ICONS[candidate.track] ?? 'book'} size={18} />
                <span className="track-chip__name">{candidate.name}</span>
                <span className="track-chip__count">{count}</span>
              </button>
            );
          })}
        </div>

        {topics.length > 0 && (
          <div className="catalogue-filters__topics" role="group" aria-label={t('courses.topicsFilter')}>
            <button
              type="button"
              className={['topic-chip', topicSlug === null && 'topic-chip--active'].filter(Boolean).join(' ')}
              aria-pressed={topicSlug === null}
              onClick={() => selectTopic(null)}
            >
              {t('courses.allTopics')}
            </button>
            {topics.map(({ topic, courses }) => (
              <button
                key={topic.slug}
                type="button"
                className={['topic-chip', topic.slug === topicSlug && 'topic-chip--active']
                  .filter(Boolean)
                  .join(' ')}
                aria-pressed={topic.slug === topicSlug}
                onClick={() => selectTopic(topic.slug)}
              >
                {topic.name}
                <span className="topic-chip__count">{courses}</span>
              </button>
            ))}
          </div>
        )}
      </section>

      {catalogue.progresses
        .filter((progress) => trackSlug === null || progress.trackSlug === trackSlug)
        .map((progress) => (
          <Panel key={progress.trackSlug} title={t('courses.progressOf', { track: progress.trackName })}>
            <TrackProgress progress={progress} />
          </Panel>
        ))}

      {catalogue.loading && catalogue.courses.length === 0 ? (
        <div className="empty">
          <Spinner size={22} label={t('courses.loading')} />
        </div>
      ) : catalogue.error ? (
        <Alert
          tone="error"
          title={t('courses.loadError')}
          action={
            <Button variant="ghost" size="sm" icon="refresh" onClick={() => void catalogue.reload()}>
              {t('common.retry')}
            </Button>
          }
        >
          {catalogue.error.message}
        </Alert>
      ) : shown.length === 0 ? (
        <p className="empty">{t(topicSlug ? 'courses.emptyTopic' : 'courses.emptyTrack')}</p>
      ) : (
        <>
          <p className="catalogue-count">{t('courses.shown', { count: shown.length })}</p>
          <div className="course-grid">
            {shown.map((course) => (
              <CourseCard key={course.slug} course={course} />
            ))}
          </div>
        </>
      )}
    </div>
  );
}
