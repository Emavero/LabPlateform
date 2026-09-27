import { useCallback, useEffect, useState, type FormEvent } from 'react';
import {
  DIFFICULTIES,
  EMPTY_BOX_DRAFT,
  OPERATING_SYSTEMS,
  type BoxDraft,
  type PublishedBox,
} from '@/domain/models/Admin';
import type { Difficulty } from '@/domain/models/Box';
import type { OperatingSystem } from '@/domain/models/VirtualMachine';
import { toAppError, type AppError } from '@/domain/errors/AppError';
import { Alert, Button, CopyField, Panel, Spinner, TextField } from '../design-system';
import { useI18n } from '../i18n/I18nContext';
import { useDependencies } from '../state/DependenciesContext';

/** Publication et retrait des machines du catalogue. */
export function AdminBoxesPage() {
  const { t } = useI18n();
  const { admin } = useDependencies();
  const [boxes, setBoxes] = useState<PublishedBox[]>([]);
  const [draft, setDraft] = useState<BoxDraft>(EMPTY_BOX_DRAFT);
  const [editing, setEditing] = useState<string | null>(null);
  const [confirming, setConfirming] = useState<string | null>(null);
  const [flags, setFlags] = useState<PublishedBox | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<AppError | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setBoxes(await admin.listBoxes.execute());
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setLoading(false);
    }
  }, [admin.listBoxes]);

  useEffect(() => {
    void reload();
  }, [reload]);

  const patch = (changes: Partial<BoxDraft>) => setDraft((current) => ({ ...current, ...changes }));

  const edit = (box: PublishedBox) => {
    setEditing(box.slug);
    setFlags(null);
    setError(null);
    setDraft({
      name: box.name,
      operatingSystem: box.os,
      difficulty: box.difficulty,
      synopsis: box.synopsis,
      ipAddress: box.ipAddress,
      maker: box.maker,
      retired: box.retired,
      proOnly: box.proOnly,
      userFlag: '',
      rootFlag: '',
    });
  };

  const reset = () => {
    setEditing(null);
    setDraft(EMPTY_BOX_DRAFT);
    setError(null);
  };

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const saved = await admin.saveBox.execute(draft, editing ?? undefined);
      setFlags(saved.userFlagOnce || saved.rootFlagOnce ? saved : null);
      reset();
      await reload();
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setSaving(false);
    }
  }

  async function remove(slug: string) {
    setError(null);
    try {
      await admin.deleteBox.execute(slug);
      setConfirming(null);
      await reload();
    } catch (e) {
      setError(toAppError(e));
    }
  }

  const fieldErrors = error?.fieldErrors ?? {};

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="page__eyebrow">{t('admin.eyebrow')}</p>
          <h1 className="page__title">{t('adminBoxes.title')}</h1>
          <p className="page__lead">{t('adminBoxes.lead')}</p>
        </div>
        <Button variant="ghost" size="sm" icon="refresh" loading={loading} onClick={() => void reload()}>
          {t('common.refresh')}
        </Button>
      </header>

      {error && Object.keys(fieldErrors).length === 0 && <Alert tone="error">{error.message}</Alert>}

      {flags && (
        <Alert tone="success" title={t('adminBoxes.flagsTitle', { name: flags.name })}>
          {t('adminBoxes.flagsText')}
          <div className="admin-flags">
            {flags.userFlagOnce && <CopyField label={t('flag.USER')} value={flags.userFlagOnce} />}
            {flags.rootFlagOnce && <CopyField label={t('flag.ROOT')} value={flags.rootFlagOnce} />}
          </div>
        </Alert>
      )}

      <Panel
        title={editing ? t('adminBoxes.editing', { name: editing }) : t('adminBoxes.new')}
        description={t(editing ? 'adminBoxes.editingHint' : 'adminBoxes.newHint')}
        actions={
          editing && (
            <Button variant="ghost" size="sm" onClick={reset}>
              {t('adminBoxes.cancelEdit')}
            </Button>
          )
        }
      >
        <form className="form" onSubmit={submit} noValidate>
          <TextField
            label={t('adminBoxes.name')}
            value={draft.name}
            onChange={(e) => patch({ name: e.target.value })}
            error={fieldErrors.name}
          />
          <div className="editor-row">
            <label className="field">
              <span className="field__label">{t('adminBoxes.system')}</span>
              <select
                className="field__input"
                value={draft.operatingSystem}
                onChange={(e) => patch({ operatingSystem: e.target.value as OperatingSystem })}
              >
                {OPERATING_SYSTEMS.map((code) => (
                  <option key={code} value={code}>
                    {t(`os.${code}`)}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span className="field__label">{t('adminBoxes.difficulty')}</span>
              <select
                className="field__input"
                value={draft.difficulty}
                onChange={(e) => patch({ difficulty: e.target.value as Difficulty })}
              >
                {DIFFICULTIES.map((code) => (
                  <option key={code} value={code}>
                    {t(`difficulty.${code}`)}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <div className="editor-row">
            <TextField
              label={t('adminBoxes.address')}
              value={draft.ipAddress}
              onChange={(e) => patch({ ipAddress: e.target.value })}
              error={fieldErrors.ipAddress}
              autoComplete="off"
              spellCheck={false}
            />
            <TextField
              label={t('adminBoxes.maker')}
              value={draft.maker}
              onChange={(e) => patch({ maker: e.target.value })}
            />
          </div>
          <label className="field">
            <span className="field__label">{t('adminBoxes.synopsis')}</span>
            <textarea
              className="field__input editor-textarea"
              rows={3}
              value={draft.synopsis}
              onChange={(e) => patch({ synopsis: e.target.value })}
              placeholder={t('adminBoxes.synopsisPlaceholder')}
            />
          </label>
          <div className="editor-row">
            <TextField
              label={t('flag.USER')}
              value={draft.userFlag}
              onChange={(e) => patch({ userFlag: e.target.value })}
              error={fieldErrors.userFlag}
              placeholder={t('adminBoxes.flagPlaceholder')}
              autoComplete="off"
              spellCheck={false}
            />
            <TextField
              label={t('flag.ROOT')}
              value={draft.rootFlag}
              onChange={(e) => patch({ rootFlag: e.target.value })}
              error={fieldErrors.rootFlag}
              placeholder={t('adminBoxes.flagPlaceholder')}
              autoComplete="off"
              spellCheck={false}
            />
          </div>
          <label className="admin-check">
            <input
              type="checkbox"
              checked={draft.retired}
              onChange={(e) => patch({ retired: e.target.checked })}
            />
            <span>{t('adminBoxes.retired')}</span>
          </label>
          <label className="admin-check">
            <input
              type="checkbox"
              checked={draft.proOnly}
              onChange={(e) => patch({ proOnly: e.target.checked })}
            />
            <span>{t('admin.proOnly')}</span>
          </label>
          <div className="editor-footer">
            <span />
            <Button type="submit" icon="check" loading={saving} loadingLabel={t('writeup.saving')}>
              {t(editing ? 'common.save' : 'adminBoxes.publish')}
            </Button>
          </div>
        </form>
      </Panel>

      <Panel title={t('adminBoxes.catalogue')} description={t('adminBoxes.count', { count: boxes.length })}>
        {loading && boxes.length === 0 ? (
          <div className="empty">
            <Spinner size={22} label={t('machines.loading')} />
          </div>
        ) : boxes.length === 0 ? (
          <p className="empty">{t('adminBoxes.empty')}</p>
        ) : (
          <ul className="admin-list">
            {boxes.map((box) => (
              <li key={box.slug} className="admin-list__item">
                <span className="admin-list__text">
                  <span className="admin-list__title">
                    {box.name} {box.retired && <span className="badge">{t('boxes.retired')}</span>}{' '}
                    {box.proOnly ? (
                      <span className="badge badge--locked">{t('boxes.lockedBadge')}</span>
                    ) : (
                      <span className="badge">{t('admin.openToAll')}</span>
                    )}
                  </span>
                  <span className="admin-list__meta">
                    {box.osName} · {box.difficultyName} · {box.totalPoints} pts · <code>{box.ipAddress}</code> ·{' '}
                    <code>/machines/{box.slug}</code>
                  </span>
                </span>
                {confirming === box.slug ? (
                  <span className="admin-list__actions">
                    <Button variant="danger" size="sm" icon="check" onClick={() => void remove(box.slug)}>
                      {t('adminBoxes.confirmDelete')}
                    </Button>
                    <Button variant="ghost" size="sm" onClick={() => setConfirming(null)}>
                      {t('common.cancel')}
                    </Button>
                  </span>
                ) : (
                  <span className="admin-list__actions">
                    <Button variant="ghost" size="sm" icon="target" onClick={() => edit(box)}>
                      {t('adminBoxes.edit')}
                    </Button>
                    <Button variant="ghost" size="sm" onClick={() => setConfirming(box.slug)}>
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
  );
}
