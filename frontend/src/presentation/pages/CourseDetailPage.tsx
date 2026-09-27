import { Link, useParams } from 'react-router-dom';
import { formatDuration, hasQuiz, nextSection, type CourseSection, type QuizResult } from '@/domain/models/Course';
import { Alert, Button, Icon, Panel, Spinner } from '../design-system';
import { QuizForm } from '../features/course/QuizForm';
import { VideoPlayer } from '../features/course/VideoPlayer';
import { useI18n } from '../i18n/I18nContext';
import { useCourse } from '../hooks/useCourse';
import { TRACK_PAGES } from '../navigation/navigation';
import { NotFoundPage } from './NotFoundPage';

/** Fiche d'un cours : le contenu de chaque section, et la case à cocher. */
export function CourseDetailPage() {
  const { t } = useI18n();
  const { track = '', slug = '' } = useParams();
  const detail = useCourse(slug);

  if (detail.loading && !detail.course) {
    return (
      <div className="page">
        <div className="empty">
          <Spinner size={22} label={t('courses.loadingCourse')} />
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
          title={t('courses.loadErrorCourse')}
          action={
            <Button variant="ghost" size="sm" icon="refresh" onClick={() => void detail.reload()}>
              {t('common.retry')}
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
            {t('course.meta', {
              level: course.levelName,
              duration: formatDuration(course.minutes),
              sections: course.sections.length,
            })}
          </p>
          <h1 className="page__title">{course.title}</h1>
          <p className="page__lead">{course.summary}</p>
        </div>
        {course.completed ? (
          <span className="badge badge--pwned">
            <Icon name="check" size={13} /> {t('course.done')}
          </span>
        ) : resume ? (
          <span className="badge">
            {t('course.sectionOf', { position: resume.position, total: course.sections.length })}
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
            result={detail.results[section.id]}
            grading={detail.grading === section.slug}
            onGrade={(answers) => void detail.gradeQuiz(section, answers)}
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
  result,
  grading,
  onGrade,
}: {
  section: CourseSection;
  pending: boolean;
  onToggle: () => void;
  result: QuizResult | undefined;
  grading: boolean;
  onGrade: (answers: Readonly<Record<number, readonly number[]>>) => void;
}) {
  const { t } = useI18n();
  return (
    <Panel
      className={['course-section', section.completed && 'course-section--done'].filter(Boolean).join(' ')}
      eyebrow={t('course.sectionMeta', {
        position: section.position,
        kind: section.kindName,
        duration: formatDuration(section.minutes),
      })}
      title={section.title}
      actions={
        // Une section à quiz se valide en rendant sa copie, pas en la cochant.
        hasQuiz(section) ? (
          section.completed && (
            <Button variant="ghost" size="sm" icon="stop" loading={pending} onClick={onToggle}>
              {t('course.reopen')}
            </Button>
          )
        ) : (
          <Button
            variant={section.completed ? 'ghost' : 'success'}
            size="sm"
            icon={section.completed ? 'stop' : 'check'}
            loading={pending}
            onClick={onToggle}
          >
            {t(section.completed ? 'course.reopen' : 'course.markDone')}
          </Button>
        )
      }
    >
      {section.videoUrl && <VideoPlayer url={section.videoUrl} title={section.title} />}
      {/* Le contenu est du texte préformaté : les commandes doivent rester lisibles telles quelles. */}
      {section.content.trim() && <pre className="course-section__content">{section.content}</pre>}
      {hasQuiz(section) && (
        <QuizForm section={section} result={result} grading={grading} onSubmit={onGrade} />
      )}
    </Panel>
  );
}
