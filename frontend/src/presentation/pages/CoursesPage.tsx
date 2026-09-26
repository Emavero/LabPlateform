import { useParams } from 'react-router-dom';
import { Alert, Button, Panel, Spinner } from '../design-system';
import { CourseCard } from '../features/course/CourseCard';
import { TrackProgress } from '../features/course/TrackProgress';
import { useCourses } from '../hooks/useCourses';
import { TRACK_PAGES } from '../navigation/navigation';
import { NotFoundPage } from './NotFoundPage';

/** Cours d'une filière. La filière vient de l'URL, donc une seule page suffit. */
export function CoursesPage() {
  const { track = '' } = useParams();
  const page = TRACK_PAGES[track];
  const courses = useCourses(track);

  if (!page) return <NotFoundPage />;

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="page__eyebrow">Cours</p>
          <h1 className="page__title">{page.title}</h1>
          <p className="page__lead">{page.description}</p>
        </div>
        <Button
          variant="ghost"
          size="sm"
          icon="refresh"
          loading={courses.loading}
          onClick={() => void courses.reload()}
        >
          Actualiser
        </Button>
      </header>

      {courses.progress && (
        <Panel title="Votre avancement">
          <TrackProgress progress={courses.progress} />
        </Panel>
      )}

      {courses.loading && courses.courses.length === 0 ? (
        <div className="empty">
          <Spinner size={22} label="Chargement des cours" />
        </div>
      ) : courses.error ? (
        <Alert
          tone="error"
          title="Impossible de charger les cours"
          action={
            <Button variant="ghost" size="sm" icon="refresh" onClick={() => void courses.reload()}>
              Réessayer
            </Button>
          }
        >
          {courses.error.message}
        </Alert>
      ) : courses.courses.length === 0 ? (
        <p className="empty">Aucun cours n'est encore publié dans cette filière.</p>
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
