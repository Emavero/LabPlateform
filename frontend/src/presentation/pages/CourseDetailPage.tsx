import { Link, useParams } from 'react-router-dom';
import { formatDuration, nextSection, type CourseSection } from '@/domain/models/Course';
import { Alert, Button, Icon, Panel, Spinner } from '../design-system';
import { useCourse } from '../hooks/useCourse';
import { TRACK_PAGES } from '../navigation/navigation';
import { NotFoundPage } from './NotFoundPage';

/** Fiche d'un cours : le contenu de chaque section, et la case à cocher. */
export function CourseDetailPage() {
  const { track = '', slug = '' } = useParams();
  const detail = useCourse(slug);

  if (detail.loading && !detail.course) {
    return (
      <div className="page">
        <div className="empty">
          <Spinner size={22} label="Chargement du cours" />
        </div>
      </div>
    );
  }
  if (detail.error?.kind === 'not_found') return <NotFoundPage />;
  // Sans cours chargé, l'erreur occupe la page ; une fois chargé, un échec de
  // bascule s'affiche en ligne pour ne pas faire disparaître le contenu.
  if (!detail.course) {
    return (
      <div className="page">
        <Alert
          tone="error"
          title="Impossible de charger ce cours"
          action={
            <Button variant="ghost" size="sm" icon="refresh" onClick={() => void detail.reload()}>
              Réessayer
            </Button>
          }
        >
          {detail.error?.message}
        </Alert>
      </div>
    );
  }

  const course = detail.course;
  const resume = nextSection(course);
  const backSlug = TRACK_PAGES[track] ? track : course.trackSlug;

  return (
    <div className="page">
      <Link className="back-link" to={`/cours/${backSlug}`}>
        <Icon name="chevronRight" size={14} /> {course.trackName}
      </Link>

      <header className="page__header">
        <div>
          <p className="page__eyebrow">
            {course.levelName} · {formatDuration(course.minutes)} · {course.sections.length} sections
          </p>
          <h1 className="page__title">{course.title}</h1>
          <p className="page__lead">{course.summary}</p>
        </div>
        {course.completed ? (
          <span className="badge badge--pwned">
            <Icon name="check" size={13} /> Cours terminé
          </span>
        ) : resume ? (
          <span className="badge">
            Section {resume.position} sur {course.sections.length}
          </span>
        ) : null}
      </header>

      {detail.error && <Alert tone="error">{detail.error.message}</Alert>}

      <div className="course-sections">
        {course.sections.map((section) => (
          <SectionPanel
            key={section.slug}
            section={section}
            pending={detail.pending === section.slug}
            onToggle={() => void detail.toggle(section)}
          />
        ))}
      </div>
    </div>
  );
}

function SectionPanel({
  section,
  pending,
  onToggle,
}: {
  section: CourseSection;
  pending: boolean;
  onToggle: () => void;
}) {
  return (
    <Panel
      className={['course-section', section.completed && 'course-section--done'].filter(Boolean).join(' ')}
      eyebrow={`${section.position}. ${section.kindName} · ${formatDuration(section.minutes)}`}
      title={section.title}
      actions={
        <Button
          variant={section.completed ? 'ghost' : 'success'}
          size="sm"
          icon={section.completed ? 'stop' : 'check'}
          loading={pending}
          onClick={onToggle}
        >
          {section.completed ? 'Rouvrir' : 'Marquer comme terminé'}
        </Button>
      }
    >
      {/* Le contenu est du texte préformaté : les commandes doivent rester lisibles telles quelles. */}
      <pre className="course-section__content">{section.content}</pre>
    </Panel>
  );
}
