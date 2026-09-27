import { useParams } from 'react-router-dom';
import { Alert, Button, Panel, Spinner } from '../design-system';
import { CourseCard } from '../features/course/CourseCard';
import { TrackProgress } from '../features/course/TrackProgress';
import { useI18n } from '../i18n/I18nContext';
import { useCourses } from '../hooks/useCourses';
import { TRACK_PAGES } from '../navigation/navigation';
import { NotFoundPage } from './NotFoundPage';

/** Cours d'une filière. La filière vient de l'URL, donc une seule page suffit. */
export function CoursesPage() {
  const { t } = useI18n();
  const { track = '' } = useParams();
  const page = TRACK_PAGES[track];
  const courses = useCourses(track);

  if (!page) return <NotFoundPage />;

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="page__eyebrow">{t('courses.eyebrow')}</p>
          <h1 className="page__title">{t(page.title)}</h1>
          <p className="page__lead">{t(page.description)}</p>
        </div>
        <Button
          variant="ghost"
          size="sm"
          icon="refresh"
          loading={courses.loading}
          onClick={() => void courses.reload()}
        >
          {t('common.refresh')}
        </Button>
      </header>

      {courses.progress && (
        <Panel title={t('courses.progress')}>
          <TrackProgress progress={courses.progress} />
        </Panel>
      )}

      {courses.loading && courses.courses.length === 0 ? (
        <div className="empty">
          <Spinner size={22} label={t('courses.loading')} />
        </div>
      ) : courses.error ? (
        <Alert
          tone="error"
          title={t('courses.loadError')}
          action={
            <Button variant="ghost" size="sm" icon="refresh" onClick={() => void courses.reload()}>
              {t('common.retry')}
            </Button>
          }
        >
          {courses.error.message}
        </Alert>
      ) : courses.courses.length === 0 ? (
        <p className="empty">{t('courses.emptyTrack')}</p>
      ) : (
        <div className="course-grid">
          {courses.courses.map((course) => (
            <CourseCard key={course.slug} course={course} />
          ))}
        </div>
      )}
    </div>
  );
}
