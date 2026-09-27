import { useState } from 'react';
import { Link } from 'react-router-dom';
import { formatDuration } from '@/domain/models/Course';
import { toAppError, type AppError } from '@/domain/errors/AppError';
import { Alert, Button, Icon, Panel, Spinner } from '../design-system';
import { useCourses } from '../hooks/useCourses';
import { useI18n } from '../i18n/I18nContext';
import { useDependencies } from '../state/DependenciesContext';
import { TRACK_PAGES } from '../navigation/navigation';

/** Liste des cours par filière, avec publication et suppression. */
export function AdminCoursesPage() {
  const { t } = useI18n();
  const forensics = useCourses('forensique');
  const defense = useCourses('defense');

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="page__eyebrow">{t('admin.eyebrow')}</p>
          <h1 className="page__title">{t('adminCourses.title')}</h1>
          <p className="page__lead">{t('adminCourses.lead')}</p>
        </div>
        <Link className="btn btn--primary btn--sm" to="/admin/cours/nouveau">
          <Icon name="book" size={16} />
          <span>{t('adminCourses.new')}</span>
        </Link>
      </header>

      <TrackCourses slug="forensique" courses={forensics} />
      <TrackCourses slug="defense" courses={defense} />
    </div>
  );
}

function TrackCourses({ slug, courses }: { slug: string; courses: ReturnType<typeof useCourses> }) {
  const { t } = useI18n();
  const { admin } = useDependencies();
  const [pending, setPending] = useState<string | null>(null);
  const [error, setError] = useState<AppError | null>(null);
  const [confirming, setConfirming] = useState<string | null>(null);

  const remove = async (courseSlug: string) => {
    setPending(courseSlug);
    setError(null);
    try {
      await admin.deleteCourse.execute(courseSlug);
      setConfirming(null);
      await courses.reload();
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setPending(null);
    }
  };

  return (
    <Panel
      title={TRACK_PAGES[slug] ? t(TRACK_PAGES[slug].title) : slug}
      description={t('adminCourses.published', { count: courses.courses.length })}
    >
      {error && <Alert tone="error">{error.message}</Alert>}
      {courses.loading && courses.courses.length === 0 ? (
        <div className="empty">
          <Spinner size={22} label={t('courses.loading')} />
        </div>
      ) : courses.error ? (
        <Alert tone="error">{courses.error.message}</Alert>
      ) : courses.courses.length === 0 ? (
        <p className="empty">{t('adminCourses.emptyTrack')}</p>
      ) : (
        <ul className="admin-list">
          {courses.courses.map((course) => (
            <li key={course.slug} className="admin-list__item">
              <span className="admin-list__text">
                <span className="admin-list__title">{course.title}</span>
                <span className="admin-list__meta">
                  {course.levelName} · {course.sections} sections · {formatDuration(course.minutes)} ·{' '}
                  <code>/cours/{course.trackSlug}/{course.slug}</code>
                </span>
              </span>
              {confirming === course.slug ? (
                <span className="admin-list__actions">
                  <Button
                    variant="danger"
                    size="sm"
                    icon="check"
                    loading={pending === course.slug}
                    onClick={() => void remove(course.slug)}
                  >
                    {t('adminCourses.confirmDelete')}
                  </Button>
                  <Button variant="ghost" size="sm" onClick={() => setConfirming(null)}>
                    Annuler
                  </Button>
                </span>
              ) : (
                <span className="admin-list__actions">
                  <Link className="btn btn--ghost btn--sm" to={`/admin/cours/${course.slug}`}>
                    Modifier
                  </Link>
                  <Button variant="ghost" size="sm" onClick={() => setConfirming(course.slug)}>
                    Supprimer
                  </Button>
                </span>
              )}
            </li>
          ))}
        </ul>
      )}
    </Panel>
  );
}
