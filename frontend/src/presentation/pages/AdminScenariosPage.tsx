import { useState, type FormEvent } from 'react';
import {
  EMPTY_SCENARIO,
  EMPTY_STEP,
  MAX_STEPS,
  STEP_KINDS,
  STEP_OBJECTIVES,
  hasBrokenStep,
  toDraft,
  type Scenario,
  type ScenarioDraft,
  type ScenarioObjective,
  type ScenarioStepKind,
  type StepDraft,
} from '@/domain/models/Scenario';
import { Alert, Button, Icon, Panel, Spinner, TextField } from '../design-system';
import { useI18n } from '../i18n/I18nContext';
import { useAdminScenarios } from '../hooks/useScenarios';

/**
 * Conception des scénarios.
 * <p>
 * Un scénario n'ajoute aucun contenu : il ordonne des machines et des cours qui
 * existent déjà, désignés par leur lien. L'éditeur ne propose donc pas de
 * créer une ressource, et le serveur refuse un lien qu'il ne connaît pas —
 * publier une étape infranchissable ferait croire au joueur que la plateforme
 * est cassée.
 */
export function AdminScenariosPage() {
  const { t, formatDate } = useI18n();
  const { scenarios, loading, busy, error, save, remove } = useAdminScenarios();
  const [draft, setDraft] = useState<ScenarioDraft>(EMPTY_SCENARIO);
  const [editing, setEditing] = useState<Scenario | null>(null);
  const [saved, setSaved] = useState(false);
  const [confirming, setConfirming] = useState<string | null>(null);

  const fieldErrors = error?.fieldErrors ?? {};
  const patch = (changes: Partial<ScenarioDraft>) => setDraft((current) => ({ ...current, ...changes }));
  const patchStep = (index: number, changes: Partial<StepDraft>) =>
    setDraft((current) => ({
      ...current,
      steps: current.steps.map((step, position) => (position === index ? { ...step, ...changes } : step)),
    }));

  const edit = (scenario: Scenario) => {
    setEditing(scenario);
    setDraft(toDraft(scenario));
    setSaved(false);
  };

  const reset = () => {
    setEditing(null);
    setDraft(EMPTY_SCENARIO);
    setSaved(false);
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setSaved(false);
    if (await save(draft, editing?.slug)) {
      reset();
      setSaved(true);
    }
  };

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="page__eyebrow">{t('admin.eyebrow')}</p>
          <h1 className="page__title">{t('scenario.manage')}</h1>
          <p className="page__lead">{t('scenario.manageLead')}</p>
        </div>
      </header>

      {error && Object.keys(fieldErrors).length === 0 && <Alert tone="error">{error.message}</Alert>}
      {saved && <Alert tone="success">{t('scenario.saved')}</Alert>}

      <div className="page__grid">
        <Panel
          title={editing ? t('scenario.editing', { name: editing.title }) : t('scenario.new')}
          actions={
            editing ? (
              <Button variant="ghost" size="sm" onClick={reset}>
                {t('scenario.cancelEdit')}
              </Button>
            ) : undefined
          }
        >
          <form className="form" onSubmit={submit} noValidate>
            <TextField
              label={t('scenario.titleField')}
              value={draft.title}
              onChange={(event) => patch({ title: event.target.value })}
              error={fieldErrors.title}
            />
            <label className="field">
              <span className="field__label">{t('scenario.brief')}</span>
              <textarea
                className="field__input editor-textarea"
                rows={4}
                value={draft.brief}
                onChange={(event) => patch({ brief: event.target.value })}
                placeholder={t('scenario.briefPlaceholder')}
              />
              {fieldErrors.brief && <span className="field__error">{fieldErrors.brief}</span>}
            </label>

            {fieldErrors.steps && <Alert tone="error">{fieldErrors.steps}</Alert>}

            {draft.steps.map((step, index) => (
              <fieldset className="step-editor" key={index}>
                <legend>{t('scenario.step', { position: index + 1 })}</legend>
                <div className="editor-row">
                  <label className="field">
                    <span className="field__label">{t('scenario.stepKind')}</span>
                    <select
                      className="field__input"
                      value={step.kind}
                      onChange={(event) => patchStep(index, { kind: event.target.value as ScenarioStepKind })}
                    >
                      {STEP_KINDS.map((kind) => (
                        <option key={kind} value={kind}>
                          {t(`scenario.kind.${kind}`)}
                        </option>
                      ))}
                    </select>
                  </label>
                  {step.kind === 'MACHINE' && (
                    <label className="field">
                      <span className="field__label">{t('scenario.objective')}</span>
                      <select
                        className="field__input"
                        value={step.objective}
                        onChange={(event) =>
                          patchStep(index, { objective: event.target.value as ScenarioObjective })
                        }
                      >
                        {STEP_OBJECTIVES.map((objective) => (
                          <option key={objective} value={objective}>
                            {t(`scenario.objective.${objective}`)}
                          </option>
                        ))}
                      </select>
                    </label>
                  )}
                </div>
                <TextField
                  label={t('scenario.reference')}
                  value={step.reference}
                  onChange={(event) => patchStep(index, { reference: event.target.value })}
                  error={fieldErrors[`step-${index}`]}
                  hint={t(step.kind === 'MACHINE' ? 'scenario.referenceHintMachine' : 'scenario.referenceHintCourse')}
                  autoComplete="off"
                  spellCheck={false}
                />
                <label className="field">
                  <span className="field__label">{t('scenario.instruction')}</span>
                  <textarea
                    className="field__input editor-textarea"
                    rows={2}
                    value={step.instruction}
                    onChange={(event) => patchStep(index, { instruction: event.target.value })}
                    placeholder={t('scenario.instructionPlaceholder')}
                  />
                </label>
                <Button
                  variant="ghost"
                  size="sm"
                  disabled={draft.steps.length === 1}
                  onClick={() => patch({ steps: draft.steps.filter((_, position) => position !== index) })}
                >
                  {t('scenario.removeStep')}
                </Button>
              </fieldset>
            ))}

            <Button
              variant="ghost"
              icon="book"
              disabled={draft.steps.length >= MAX_STEPS}
              onClick={() => patch({ steps: [...draft.steps, EMPTY_STEP] })}
            >
              {t('scenario.addStep')}
            </Button>

            <label className="admin-check">
              <input
                type="checkbox"
                checked={draft.published}
                onChange={(event) => patch({ published: event.target.checked })}
              />
              <span>{t('scenario.publishedLabel')}</span>
            </label>

            <Button type="submit" icon="check" loading={busy} loadingLabel={t('writeup.saving')}>
              {t(editing ? 'common.save' : 'scenario.publish')}
            </Button>
          </form>
        </Panel>

        <Panel title={t('scenario.list')} description={t('scenario.count', { count: scenarios.length })}>
          {loading && scenarios.length === 0 ? (
            <div className="empty">
              <Spinner size={22} label={t('scenario.loading')} />
            </div>
          ) : scenarios.length === 0 ? (
            <p className="empty">{t('scenario.empty')}</p>
          ) : (
            <ul className="admin-list">
              {scenarios.map((scenario) => (
                <li key={scenario.slug} className="admin-list__row">
                  <span className="admin-list__main">
                    <span className="admin-list__title">
                      {scenario.title}{' '}
                      {scenario.published ? (
                        <span className="badge">{t('scenario.publishedShort')}</span>
                      ) : (
                        <span className="badge badge--locked">{t('scenario.draftLabel')}</span>
                      )}
                      {hasBrokenStep(scenario) && (
                        <span className="badge badge--locked">
                          <Icon name="alert" size={12} /> {t('scenario.brokenWarning')}
                        </span>
                      )}
                    </span>
                    <span className="admin-list__meta">
                      {t('scenario.stepCount', { count: scenario.steps.length })} · {formatDate(scenario.updatedAt)} ·{' '}
                      <code>/modules/scenario-designer/{scenario.slug}</code>
                    </span>
                  </span>
                  {confirming === scenario.slug ? (
                    <span className="admin-list__actions">
                      <Button
                        variant="danger"
                        size="sm"
                        icon="check"
                        loading={busy}
                        onClick={() => void remove(scenario.slug).then(() => setConfirming(null))}
                      >
                        {t('scenario.confirmDelete')}
                      </Button>
                      <Button variant="ghost" size="sm" onClick={() => setConfirming(null)}>
                        {t('common.cancel')}
                      </Button>
                    </span>
                  ) : (
                    <span className="admin-list__actions">
                      <Button variant="ghost" size="sm" icon="target" onClick={() => edit(scenario)}>
                        {t('adminBoxes.edit')}
                      </Button>
                      <Button variant="ghost" size="sm" onClick={() => setConfirming(scenario.slug)}>
                        {t('common.delete')}
                      </Button>
                    </span>
                  )}
                </li>
              ))}
            </ul>
          )}
        </Panel>
      </div>
    </div>
  );
}
