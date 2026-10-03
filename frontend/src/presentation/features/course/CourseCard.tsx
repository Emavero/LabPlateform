import { Link } from 'react-router-dom';
import { courseRatio, formatDuration, type CourseSummary } from '@/domain/models/Course';
import { Icon } from '../../design-system';
import { useI18n } from '../../i18n/I18nContext';

/** Vignette d'un cours : niveau, durée, et où l'on en est. */
export function CourseCard({ course }: { course: CourseSummary }) {
  const { t } = useI18n();
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
            {t('course.meta', {
              level: course.levelName,
              duration: formatDuration(course.minutes),
              sections: course.sections,
            })}
          </p>
          <h3 className="course-card__title" id={titleId}>
            <Link to={`/cours/${course.trackSlug}/${course.slug}`}>{course.title}</Link>
          </h3>
        </div>
        {course.completed ? (
          <span className="badge badge--pwned">
            <Icon name="check" size={13} /> {t('course.finished')}
          </span>
        ) : course.started ? (
          <span className="badge">{t('course.inProgress')}</span>
        ) : null}
      </header>

      <p className="course-card__topic">
        <Icon name="layers" size={13} /> {course.topicName}
      </p>

      <p className="course-card__summary">{course.summary}</p>

      <div className="course-card__progress">
        <div className="progress__bar" role="img" aria-label={t('course.percentDone', { percent })}>
          <span className="progress__bar-fill" style={{ width: `${percent}%` }} />
        </div>
        <span className="course-card__count">
          {course.sectionsCompleted} / {course.sections}
        </span>
      </div>

      <footer className="course-card__footer">
        <Link className="text-link" to={`/cours/${course.trackSlug}/${course.slug}`}>
          {t(course.started ? 'course.resume' : 'course.start')} <Icon name="chevronRight" size={14} />
        </Link>
      </footer>
    </article>
  );
}
