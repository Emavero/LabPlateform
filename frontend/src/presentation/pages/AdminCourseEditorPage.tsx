import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  EMPTY_CASE,
  EMPTY_DESIGNER,
  EMPTY_DRAFT,
  COURSE_LEVELS,
  EMPTY_QUESTION,
  EMPTY_SECTION,
  EMPTY_STAGE,
  SECTION_KINDS,
  type CaseDraft,
  type CourseDraft,
  type DesignerDraft,
  type QuestionDraft,
  type SectionDraft,
  type StageDraft,
} from '@/domain/models/Admin';
import type {
  Course,
  CourseLevel,
  SectionKind,
  TopicCode,
  Track,
} from '@/domain/models/Course';
import { validateDraft } from '@/domain/validation/courseDraft';
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
  const { admin, courses } = useDependencies();

  const [draft, setDraft] = useState<CourseDraft>(EMPTY_DRAFT);
  const [tracks, setTracks] = useState<Track[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<AppError | null>(null);
  const [saved, setSaved] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      // Les filières et leurs sous-domaines viennent du serveur : les recopier
      // ici reviendrait à tenir une deuxième liste, qui finirait par mentir.
      const [allTracks, course] = await Promise.all([
        courses.tracks.execute(),
        // Lecture d'administration : c'est la seule qui révèle les bonnes réponses.
        editing && slug ? admin.getCourse.execute(slug) : Promise.resolve(null),
      ]);
      setTracks(allTracks);
      if (course) setDraft(toDraft(course));
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setLoading(false);
    }
  }, [admin.getCourse, courses.tracks, editing, slug]);

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

  const patchStage = (index: number, changes: Partial<StageDraft>) =>
    patch({ stages: draft.stages.map((stage, i) => (i === index ? { ...stage, ...changes } : stage)) });

  const patchDesigner = (index: number, changes: Partial<DesignerDraft>) =>
    patch({
      designers: draft.designers.map((designer, i) => (i === index ? { ...designer, ...changes } : designer)),
    });

  const patchCase = (changes: Partial<CaseDraft>) => patch({ realCase: { ...draft.realCase, ...changes } });

  /** Déplace une étape d'un cran : l'ordre de la liste fait l'ordre de la chaîne. */
  const moveStage = (index: number, direction: -1 | 1) => {
    const target = index + direction;
    if (target < 0 || target >= draft.stages.length) return;
    const stages = [...draft.stages];
    [stages[index], stages[target]] = [stages[target], stages[index]];
    patch({ stages });
  };

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
  // La filière du cours est celle de son sous-domaine : rien à tenir à jour.
  const selectedTrack = tracks.find((track) => track.topics.some((topic) => topic.topic === draft.topic));
  // Les règles du dossier sont celles du serveur : les dire ici évite d'envoyer
  // un formulaire qu'il refusera.
  const errors = validateDraft(draft);

  /** Changer de filière emmène vers son premier sous-domaine : l'ancien n'y existe pas. */
  const selectTrack = (slug: string) => {
    const first = tracks.find((track) => track.slug === slug)?.topics[0];
    if (first) patch({ topic: first.topic });
  };

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
            /cours/{selectedTrack?.slug ?? ''}/{saved}
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
          {/* La filière n'est pas enregistrée : elle sert à raccourcir la liste des
              sous-domaines, qui est ce que le cours porte vraiment. */}
          <div className="editor-row">
            <label className="field">
              <span className="field__label">{t('editor.track')}</span>
              <select
                className="field__input"
                value={selectedTrack?.slug ?? ''}
                onChange={(e) => selectTrack(e.target.value)}
              >
                {tracks.map((track) => (
                  <option key={track.slug} value={track.slug}>
                    {track.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span className="field__label">{t('editor.topic')}</span>
              <select
                className="field__input"
                value={draft.topic}
                onChange={(e) => patch({ topic: e.target.value as TopicCode })}
              >
                {(selectedTrack?.topics ?? []).map((topic) => (
                  <option key={topic.topic} value={topic.topic}>
                    {topic.name}
                  </option>
                ))}
              </select>
              <span className="field__hint">{t('editor.topicHint')}</span>
            </label>
          </div>
          <div className="editor-row">
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
              {/* Le sommaire de la page de cours se construit tout seul : il faut
                  donc dire ici comment on y fait apparaître un sous-titre. */}
              <span className="field__hint">{t('editor.contentHint')}</span>
            </label>
          </Panel>
        ))}

        {/* Le dossier du cours : chaîne d'attaque étudiée, situation réelle où
            elle se rencontre, et ceux qui l'ont écrite. Tout est facultatif —
            un cours reste publiable sans, et la page de cours s'en passe. */}
        <Panel
          title={t('editor.attackPath')}
          description={t('editor.attackPathHint')}
          actions={
            <Button variant="ghost" size="sm" icon="route" onClick={() => patch({ stages: [...draft.stages, EMPTY_STAGE] })}>
              {t('editor.addStage')}
            </Button>
          }
        >
          {errors.attackPath && <Alert tone="error">{errors.attackPath}</Alert>}
          <label className="field">
            <span className="field__label">{t('editor.attackSummary')}</span>
            <textarea
              className="field__input editor-textarea"
              rows={3}
              value={draft.attackSummary}
              onChange={(e) => patch({ attackSummary: e.target.value })}
              placeholder={t('editor.attackSummaryPlaceholder')}
            />
          </label>

          {draft.stages.map((stage, index) => (
            <div className="editor-sub" key={index}>
              <div className="editor-sub__head">
                <p className="field__label">{t('editor.stage', { number: index + 1 })}</p>
                <span className="editor-actions">
                  <Button variant="ghost" size="sm" onClick={() => moveStage(index, -1)} disabled={index === 0}>
                    ↑
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => moveStage(index, 1)}
                    disabled={index === draft.stages.length - 1}
                  >
                    ↓
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => patch({ stages: draft.stages.filter((_, i) => i !== index) })}
                  >
                    {t('editor.remove')}
                  </Button>
                </span>
              </div>
              <div className="editor-row">
                <TextField
                  label={t('editor.stageName')}
                  value={stage.name}
                  onChange={(e) => patchStage(index, { name: e.target.value })}
                  placeholder={t('editor.stageNamePlaceholder')}
                />
                <TextField
                  label={t('editor.stageTechnique')}
                  value={stage.technique}
                  onChange={(e) => patchStage(index, { technique: e.target.value })}
                  placeholder={t('editor.stageTechniquePlaceholder')}
                  hint={t('editor.stageTechniqueHint')}
                />
              </div>
              <label className="field">
                <span className="field__label">{t('editor.stageDescription')}</span>
                <textarea
                  className="field__input editor-textarea"
                  rows={2}
                  value={stage.description}
                  onChange={(e) => patchStage(index, { description: e.target.value })}
                />
              </label>
            </div>
          ))}
        </Panel>

        <Panel title={t('editor.realCase')} description={t('editor.realCaseHint')}>
          {errors.realCase && <Alert tone="error">{errors.realCase}</Alert>}
          <TextField
            label={t('editor.caseSector')}
            value={draft.realCase.sector}
            onChange={(e) => patchCase({ sector: e.target.value })}
            placeholder={t('editor.caseSectorPlaceholder')}
          />
          <label className="field">
            <span className="field__label">{t('course.caseSituation')}</span>
            <textarea
              className="field__input editor-textarea"
              rows={3}
              value={draft.realCase.situation}
              onChange={(e) => patchCase({ situation: e.target.value })}
            />
          </label>
          <label className="field">
            <span className="field__label">{t('course.caseStake')}</span>
            <textarea
              className="field__input editor-textarea"
              rows={2}
              value={draft.realCase.stake}
              onChange={(e) => patchCase({ stake: e.target.value })}
            />
          </label>
          <label className="field">
            <span className="field__label">{t('course.caseOutcome')}</span>
            <textarea
              className="field__input editor-textarea"
              rows={2}
              value={draft.realCase.outcome}
              onChange={(e) => patchCase({ outcome: e.target.value })}
            />
          </label>
        </Panel>

        <Panel
          title={t('editor.designers')}
          description={t('editor.designersHint')}
          actions={
            <Button
              variant="ghost"
              size="sm"
              icon="user"
              onClick={() => patch({ designers: [...draft.designers, EMPTY_DESIGNER] })}
            >
              {t('editor.addDesigner')}
            </Button>
          }
        >
          {errors.designers && <Alert tone="error">{errors.designers}</Alert>}
          {draft.designers.length === 0 && <p className="empty">{t('editor.noDesigner')}</p>}
          {draft.designers.map((designer, index) => (
            <div className="editor-sub" key={index}>
              <div className="editor-sub__head">
                <p className="field__label">{t('editor.designer', { number: index + 1 })}</p>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => patch({ designers: draft.designers.filter((_, i) => i !== index) })}
                >
                  {t('editor.remove')}
                </Button>
              </div>
              <div className="editor-row">
                <TextField
                  label={t('editor.designerName')}
                  value={designer.name}
                  onChange={(e) => patchDesigner(index, { name: e.target.value })}
                />
                <TextField
                  label={t('editor.designerRole')}
                  value={designer.role}
                  onChange={(e) => patchDesigner(index, { role: e.target.value })}
                  placeholder={t('editor.designerRolePlaceholder')}
                />
              </div>
              <TextField
                label={t('editor.designerAvatar')}
                value={designer.avatarUrl}
                onChange={(e) => patchDesigner(index, { avatarUrl: e.target.value })}
                placeholder={t('editor.designerAvatarPlaceholder')}
                hint={t('editor.designerAvatarHint')}
                autoComplete="off"
                spellCheck={false}
              />
            </div>
          ))}
        </Panel>

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
    topic: course.topic,
    level: course.level,
    summary: course.summary,
    attackSummary: course.attackPath?.summary ?? '',
    stages: (course.attackPath?.stages ?? []).map((stage) => ({
      name: stage.name,
      description: stage.description,
      technique: stage.technique ?? '',
    })),
    realCase: course.realCase ?? EMPTY_CASE,
    designers: course.designers.map((designer) => ({
      name: designer.name,
      role: designer.role,
      avatarUrl: designer.avatarUrl ?? '',
    })),
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
