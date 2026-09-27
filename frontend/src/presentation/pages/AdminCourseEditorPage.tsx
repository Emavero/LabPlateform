import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  EMPTY_DRAFT,
  COURSE_LEVELS,
  EMPTY_QUESTION,
  EMPTY_SECTION,
  SECTION_KINDS,
  TRACK_CODES,
  type CourseDraft,
  type QuestionDraft,
  type SectionDraft,
} from '@/domain/models/Admin';
import type { Course, CourseLevel, SectionKind, TrackCode } from '@/domain/models/Course';
import { toAppError, type AppError } from '@/domain/errors/AppError';
import { Alert, Button, Icon, Panel, Spinner, TextField } from '../design-system';
import { VideoUpload } from '../features/course/VideoUpload';
import { useI18n } from '../i18n/I18nContext';
import { useDependencies } from '../state/DependenciesContext';

/** Éditeur d'un cours : en-tête, puis les sections dans l'ordre de lecture. */
export function AdminCourseEditorPage() {
  const { t, tm } = useI18n();
  const { slug } = useParams();
  const editing = slug !== undefined && slug !== 'nouveau';
  const navigate = useNavigate();
  const { admin } = useDependencies();

  const [draft, setDraft] = useState<CourseDraft>(EMPTY_DRAFT);
  const [loading, setLoading] = useState(editing);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<AppError | null>(null);
  const [saved, setSaved] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!editing || !slug) return;
    setLoading(true);
    setError(null);
    try {
      // Lecture d'administration : c'est la seule qui révèle les bonnes réponses.
      setDraft(toDraft(await admin.getCourse.execute(slug)));
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setLoading(false);
    }
  }, [admin.getCourse, editing, slug]);

  useEffect(() => {
    void load();
  }, [load]);

  const patch = (changes: Partial<CourseDraft>) => setDraft((current) => ({ ...current, ...changes }));

  const patchSection = (index: number, changes: Partial<SectionDraft>) =>
    setDraft((current) => ({
      ...current,
      sections: current.sections.map((section, i) => (i === index ? { ...section, ...changes } : section)),
    }));

  const addSection = () => patch({ sections: [...draft.sections, EMPTY_SECTION] });

  const patchQuestions = (index: number, questions: QuestionDraft[]) => patchSection(index, { questions });

  const removeSection = (index: number) =>
    patch({ sections: draft.sections.filter((_, i) => i !== index) });

  /** Déplace une section d'un cran : l'ordre de la liste fait l'ordre de lecture. */
  const moveSection = (index: number, direction: -1 | 1) => {
    const target = index + direction;
    if (target < 0 || target >= draft.sections.length) return;
    const sections = [...draft.sections];
    [sections[index], sections[target]] = [sections[target], sections[index]];
    patch({ sections });
  };

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    setSaved(null);
    try {
      const course = await admin.saveCourse.execute(draft, editing ? slug : undefined);
      setSaved(course.slug);
      setDraft(toDraft(course));
      if (!editing) navigate(`/admin/cours/${course.slug}`, { replace: true });
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <div className="page">
        <div className="empty">
          <Spinner size={22} label={t('courses.loadingCourse')} />
        </div>
      </div>
    );
  }

  const fieldErrors = error?.fieldErrors ?? {};

  return (
    <div className="page">
      <button type="button" className="back-link" onClick={() => navigate('/admin/cours')}>
        <Icon name="chevronRight" size={14} /> {t('adminCourses.title')}
      </button>

      <header className="page__header">
        <div>
          <p className="page__eyebrow">{t('admin.eyebrow')}</p>
          <h1 className="page__title">{t(editing ? 'editor.editTitle' : 'editor.newTitle')}</h1>
          <p className="page__lead">{t(editing ? 'editor.editHint' : 'editor.newHint')}</p>
        </div>
      </header>

      {error && Object.keys(fieldErrors).length === 0 && <Alert tone="error">{error.message}</Alert>}
      {saved && (
        <Alert tone="success" title={t('editor.saved')}>
          {t('editor.savedText')}{' '}
          <code>
            /cours/{draft.track === 'FORENSICS' ? 'forensique' : 'defense'}/{saved}
          </code>
        </Alert>
      )}

      <form className="form" onSubmit={submit} noValidate>
        <Panel title={t('editor.course')}>
          <TextField
            label={t('editor.title')}
            value={draft.title}
            onChange={(e) => patch({ title: e.target.value })}
            error={fieldErrors.title}
            hint={t('editor.titleHint')}
          />
          <div className="editor-row">
            <label className="field">
              <span className="field__label">{t('editor.track')}</span>
              <select
                className="field__input"
                value={draft.track}
                onChange={(e) => patch({ track: e.target.value as TrackCode })}
              >
                {TRACK_CODES.map((code) => (
                  <option key={code} value={code}>
                    {t(`track.${code}`)}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span className="field__label">{t('editor.level')}</span>
              <select
                className="field__input"
                value={draft.level}
                onChange={(e) => patch({ level: e.target.value as CourseLevel })}
              >
                {COURSE_LEVELS.map((code) => (
                  <option key={code} value={code}>
                    {t(`courses.level.${code}`)}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <label className="field">
            <span className="field__label">{t('editor.summary')}</span>
            <textarea
              className="field__input editor-textarea"
              rows={3}
              value={draft.summary}
              onChange={(e) => patch({ summary: e.target.value })}
              placeholder={t('editor.summaryPlaceholder')}
            />
            {fieldErrors.summary && <span className="field__error">{tm(fieldErrors.summary)}</span>}
          </label>
        </Panel>

        {draft.sections.map((section, index) => (
          <Panel
            key={index}
            eyebrow={t('editor.section', { number: index + 1 })}
            title={section.title || t('editor.untitled')}
            actions={
              <span className="editor-actions">
                <Button variant="ghost" size="sm" onClick={() => moveSection(index, -1)} disabled={index === 0}>
                  ↑
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => moveSection(index, 1)}
                  disabled={index === draft.sections.length - 1}
                >
                  ↓
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => removeSection(index)}
                  disabled={draft.sections.length === 1}
                >
                  {t('editor.remove')}
                </Button>
              </span>
            }
          >
            {fieldErrors[`section-${index}`] && <Alert tone="error">{fieldErrors[`section-${index}`]}</Alert>}
            <TextField
              label={t('editor.sectionTitle')}
              value={section.title}
              onChange={(e) => patchSection(index, { title: e.target.value })}
            />
            <div className="editor-row">
              <label className="field">
                <span className="field__label">{t('editor.kind')}</span>
                <select
                  className="field__input"
                  value={section.kind}
                  onChange={(e) => patchSection(index, { kind: e.target.value as SectionKind })}
                >
                  {SECTION_KINDS.map((code) => (
                    <option key={code} value={code}>
                      {t(`courses.kind.${code}`)}
                    </option>
                  ))}
                </select>
              </label>
              <TextField
                label={t('editor.minutes')}
                type="number"
                min={0}
                max={600}
                value={section.minutes}
                onChange={(e) => patchSection(index, { minutes: Number(e.target.value) })}
              />
            </div>
            <TextField
              label={t('editor.video')}
              value={section.videoUrl}
              onChange={(e) => patchSection(index, { videoUrl: e.target.value })}
              placeholder={t('editor.videoPlaceholder')}
              hint={t('editor.videoHint')}
              autoComplete="off"
              spellCheck={false}
            />
            <VideoUpload onUploaded={(url) => patchSection(index, { videoUrl: url })} />
            <QuestionsEditor
              questions={section.questions}
              onChange={(questions) => patchQuestions(index, questions)}
            />
            <label className="field">
              <span className="field__label">{t('editor.content')}</span>
              <textarea
                className="field__input editor-textarea editor-textarea--tall"
                rows={10}
                value={section.content}
                onChange={(e) => patchSection(index, { content: e.target.value })}
                placeholder={t('editor.contentPlaceholder')}
              />
            </label>
          </Panel>
        ))}

        <div className="editor-footer">
          <Button variant="ghost" icon="book" onClick={addSection}>
            {t('editor.addSection')}
          </Button>
          <Button type="submit" icon="check" loading={saving} loadingLabel={t('writeup.saving')}>
            {t(editing ? 'editor.saveChanges' : 'editor.publishCourse')}
          </Button>
        </div>
      </form>
    </div>
  );
}

/**
 * Questions d'une section. Les propositions cochées « correcte » sont les
 * bonnes réponses : le serveur en exige au moins une.
 */
function QuestionsEditor({
  questions,
  onChange,
}: {
  questions: readonly QuestionDraft[];
  onChange: (questions: QuestionDraft[]) => void;
}) {
  const { t } = useI18n();
  const patchQuestion = (index: number, changes: Partial<QuestionDraft>) =>
    onChange(questions.map((question, i) => (i === index ? { ...question, ...changes } : question)));

  return (
    <div className="quiz-editor">
      <p className="field__label">{t('editor.quiz', { count: questions.length })}</p>

      {questions.map((question, index) => (
        <div key={index} className="quiz-editor__question">
          <div className="quiz-editor__row">
            <TextField
              label={t('editor.question', { number: index + 1 })}
              value={question.statement}
              onChange={(e) => patchQuestion(index, { statement: e.target.value })}
            />
            <Button
              variant="ghost"
              size="sm"
              onClick={() => onChange(questions.filter((_, i) => i !== index))}
            >
              {t('editor.removeQuestion')}
            </Button>
          </div>

          {question.choices.map((choice, choiceIndex) => (
            <div key={choiceIndex} className="quiz-editor__choice">
              <input
                className="field__input"
                value={choice.label}
                placeholder={t('editor.choice', { number: choiceIndex + 1 })}
                onChange={(e) =>
                  patchQuestion(index, {
                    choices: question.choices.map((current, i) =>
                      i === choiceIndex ? { ...current, label: e.target.value } : current,
                    ),
                  })
                }
              />
              <label className="admin-check">
                <input
                  type="checkbox"
                  checked={choice.correct}
                  onChange={(e) =>
                    patchQuestion(index, {
                      choices: question.choices.map((current, i) =>
                        i === choiceIndex ? { ...current, correct: e.target.checked } : current,
                      ),
                    })
                  }
                />
                <span>{t('editor.correct')}</span>
              </label>
              <Button
                variant="ghost"
                size="sm"
                disabled={question.choices.length <= 2}
                onClick={() =>
                  patchQuestion(index, { choices: question.choices.filter((_, i) => i !== choiceIndex) })
                }
              >
                ×
              </Button>
            </div>
          ))}

          <Button
            variant="ghost"
            size="sm"
            disabled={question.choices.length >= 6}
            onClick={() =>
              patchQuestion(index, { choices: [...question.choices, { label: '', correct: false }] })
            }
          >
            {t('editor.addChoice')}
          </Button>
        </div>
      ))}

      <Button variant="ghost" size="sm" icon="book" onClick={() => onChange([...questions, EMPTY_QUESTION])}>
        {t('editor.addQuestion')}
      </Button>
    </div>
  );
}

/** Le cours relu devient un brouillon : les identifiants de section sont conservés. */
function toDraft(course: Course): CourseDraft {
  return {
    title: course.title,
    track: course.track,
    level: course.level,
    summary: course.summary,
    sections: course.sections.map((section) => ({
      id: section.id,
      title: section.title,
      kind: section.kind,
      minutes: section.minutes,
      content: section.content,
      videoUrl: section.videoUrl ?? '',
      questions: section.questions.map((question) => ({
        statement: question.statement,
        choices: question.choices.map((choice) => ({ label: choice.label, correct: choice.correct === true })),
      })),
    })),
  };
}
