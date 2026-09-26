import { Link } from 'react-router-dom';
import { courseRatio, formatDuration, type CourseSummary } from '@/domain/models/Course';
import { Icon } from '../../design-system';

/** Vignette d'un cours : niveau, durée, et où l'on en est. */
export function CourseCard({ course }: { course: CourseSummary }) {
  const percent = Math.round(courseRatio(course) * 100);
  const titleId = `course-${course.slug}-title`;

  return (
    <article
      className={['course-card', course.completed && 'course-card--done'].filter(Boolean).join(' ')}
      aria-labelledby={titleId}
    >
      <header className="course-card__header">
        <div>
          <p className="course-card__level">
            {course.levelName} · {formatDuration(course.minutes)} · {course.sections} sections
          </p>
          <h3 className="course-card__title" id={titleId}>
            <Link to={`/cours/${course.trackSlug}/${course.slug}`}>{course.title}</Link>
          </h3>
        </div>
        {course.completed ? (
          <span className="badge badge--pwned">
            <Icon name="check" size={13} /> Terminé
          </span>
        ) : course.started ? (
          <span className="badge">En cours</span>
        ) : null}
      </header>

      <p className="course-card__summary">{course.summary}</p>

      <div className="course-card__progress">
        <div className="progress__bar" role="img" aria-label={`${percent} % du cours terminé`}>
          <span className="progress__bar-fill" style={{ width: `${percent}%` }} />
        </div>
        <span className="course-card__count">
          {course.sectionsCompleted} / {course.sections}
        </span>
      </div>

      <footer className="course-card__footer">
        <Link className="text-link" to={`/cours/${course.trackSlug}/${course.slug}`}>
          {course.started ? 'Reprendre' : 'Commencer'} <Icon name="chevronRight" size={14} />
        </Link>
      </footer>
    </article>
  );
}
