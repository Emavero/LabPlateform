import { useMemo } from 'react';
import { Link, useParams } from 'react-router-dom';
import { formatDuration, hasQuiz, nextSection, type CourseSection, type QuizResult } from '@/domain/models/Course';
import {
  courseOutline,
  sectionAnchor,
  sectionBlocks,
  type OutlineEntry,
} from '@/domain/models/CourseOutline';
import { Alert, Button, Icon, Panel, Spinner } from '../design-system';
import { AttackPathPanel } from '../features/course/AttackPathPanel';
import { CourseOutlinePanel } from '../features/course/CourseOutlinePanel';
import { DesignersPanel } from '../features/course/DesignersPanel';
import { QuizForm } from '../features/course/QuizForm';
import { RealCasePanel } from '../features/course/RealCasePanel';
import { VideoPlayer } from '../features/course/VideoPlayer';
import { useI18n } from '../i18n/I18nContext';
import { useCourse } from '../hooks/useCourse';
import { NotFoundPage } from './NotFoundPage';

/** Ancres des sections que la page ajoute au contenu du cours. */
const ATTACK_PATH_ID = 'chemin-d-attaque';
const REAL_CASE_ID = 'cas-d-usage-reel';
const DESIGNERS_ID = 'concepteurs-du-scenario';

/** Fiche d'un cours : son contenu, son sommaire, et ce qui l'entoure. */
export function CourseDetailPage() {
  const { t } = useI18n();
  const { track = '', slug = '' } = useParams();
  const detail = useCourse(slug);
  const course = detail.course;

  /**
   * Le sommaire mêle ce que le cours contient — sections et sous-titres, que le
   * domaine sait extraire — et ce que la page ajoute, dont les intitulés sont
   * traduits. D'où l'assemblage ici plutôt que dans le domaine.
   */
  const outline = useMemo<OutlineEntry[]>(() => {
    if (!course) return [];
    return [
      ...courseOutline(course),
      ...(course.attackPath ? [{ id: ATTACK_PATH_ID, label: t('course.attackPath'), level: 2 as const }] : []),
      ...(course.realCase ? [{ id: REAL_CASE_ID, label: t('course.realCase'), level: 2 as const }] : []),
      ...(course.designers.length > 0
        ? [{ id: DESIGNERS_ID, label: t('course.designers'), level: 2 as const }]
        : []),
    ];
  }, [course, t]);

  if (detail.loading && !course) {
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
  if (!course) {
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

  const resume = nextSection(course);
  // La filière de l'URL peut être absente ou périmée : celle du cours fait foi.
  const backSlug = track || course.trackSlug;

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
          {/* Le sous-domaine mène au filtre correspondant : on repart de la page
              des cours avec le même angle que celui qu'on vient de lire. */}
          <p className="course-topic">
            <Link className="topic-chip topic-chip--link" to={`/cours/${course.trackSlug}?domaine=${course.topicSlug}`}>
              <Icon name="layers" size={14} /> {course.topicName}
            </Link>
          </p>
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

      <div className="course-layout">
        <div className="course-layout__main">
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

          {course.attackPath && <AttackPathPanel id={ATTACK_PATH_ID} path={course.attackPath} />}
          {course.realCase && <RealCasePanel id={REAL_CASE_ID} realCase={course.realCase} />}
          {course.designers.length > 0 && <DesignersPanel id={DESIGNERS_ID} designers={course.designers} />}
        </div>

        <CourseOutlinePanel entries={outline} />
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
  const blocks = sectionBlocks(section);

  return (
    <Panel
      id={sectionAnchor(section)}
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
      {/* Le texte reste préformaté : les commandes doivent rester lisibles telles
          quelles. Seuls les sous-titres en sortent, pour que le sommaire les vise. */}
      {blocks.map((block, index) =>
        block.kind === 'subtitle' ? (
          <h3 className="course-section__subtitle" id={block.id} key={block.id}>
            {block.label}
          </h3>
        ) : (
          <pre className="course-section__content" key={`text-${index}`}>
            {block.text}
          </pre>
        ),
      )}
      {hasQuiz(section) && <QuizForm section={section} result={result} grading={grading} onSubmit={onGrade} />}
    </Panel>
  );
}
