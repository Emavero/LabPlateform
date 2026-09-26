import { useCallback, useEffect, useState, type FormEvent } from 'react';
import {
  DIFFICULTY_LABELS,
  EMPTY_BOX_DRAFT,
  OS_LABELS,
  type BoxDraft,
  type PublishedBox,
} from '@/domain/models/Admin';
import type { Difficulty } from '@/domain/models/Box';
import type { OperatingSystem } from '@/domain/models/VirtualMachine';
import { toAppError, type AppError } from '@/domain/errors/AppError';
import { Alert, Button, CopyField, Panel, Spinner, TextField } from '../design-system';
import { useDependencies } from '../state/DependenciesContext';

/** Publication et retrait des machines du catalogue. */
export function AdminBoxesPage() {
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
          <p className="page__eyebrow">Administration</p>
          <h1 className="page__title">Gérer les machines</h1>
          <p className="page__lead">
            Une machine publiée ici apparaît aussitôt dans le catalogue, avec ses points et ses deux flags.
          </p>
        </div>
        <Button variant="ghost" size="sm" icon="refresh" loading={loading} onClick={() => void reload()}>
          Actualiser
        </Button>
      </header>

      {error && Object.keys(fieldErrors).length === 0 && <Alert tone="error">{error.message}</Alert>}

      {flags && (
        <Alert tone="success" title={`${flags.name} — notez ces flags maintenant`}>
          Ils ne seront plus jamais affichés : déposez-les sur la cible avant de quitter cette page.
          <div className="admin-flags">
            {flags.userFlagOnce && <CopyField label="Flag utilisateur" value={flags.userFlagOnce} />}
            {flags.rootFlagOnce && <CopyField label="Flag root" value={flags.rootFlagOnce} />}
          </div>
        </Alert>
      )}

      <Panel
        title={editing ? `Modifier « ${editing} »` : 'Nouvelle machine'}
        description={
          editing
            ? "Laisser un flag vide le laisse inchangé. Le lien de la machine ne change pas."
            : 'Laisser les flags vides les fait tirer au hasard : ils seront affichés une fois, à déposer sur la cible.'
        }
        actions={
          editing && (
            <Button variant="ghost" size="sm" onClick={reset}>
              Annuler la modification
            </Button>
          )
        }
      >
        <form className="form" onSubmit={submit} noValidate>
          <TextField
            label="Nom"
            value={draft.name}
            onChange={(e) => patch({ name: e.target.value })}
            error={fieldErrors.name}
          />
          <div className="editor-row">
            <label className="field">
              <span className="field__label">Système</span>
              <select
                className="field__input"
                value={draft.operatingSystem}
                onChange={(e) => patch({ operatingSystem: e.target.value as OperatingSystem })}
              >
                {Object.entries(OS_LABELS).map(([code, label]) => (
                  <option key={code} value={code}>
                    {label}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span className="field__label">Difficulté</span>
              <select
                className="field__input"
                value={draft.difficulty}
                onChange={(e) => patch({ difficulty: e.target.value as Difficulty })}
              >
                {Object.entries(DIFFICULTY_LABELS).map(([code, label]) => (
                  <option key={code} value={code}>
                    {label}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <div className="editor-row">
            <TextField
              label="Adresse dans le réseau du lab"
              value={draft.ipAddress}
              onChange={(e) => patch({ ipAddress: e.target.value })}
              error={fieldErrors.ipAddress}
              autoComplete="off"
              spellCheck={false}
            />
            <TextField label="Auteur" value={draft.maker} onChange={(e) => patch({ maker: e.target.value })} />
          </div>
          <label className="field">
            <span className="field__label">Synopsis</span>
            <textarea
              className="field__input editor-textarea"
              rows={3}
              value={draft.synopsis}
              onChange={(e) => patch({ synopsis: e.target.value })}
              placeholder="Ce que le joueur va rencontrer, sans donner la solution."
            />
          </label>
          <div className="editor-row">
            <TextField
              label="Flag utilisateur"
              value={draft.userFlag}
              onChange={(e) => patch({ userFlag: e.target.value })}
              error={fieldErrors.userFlag}
              placeholder="32 caractères hexadécimaux, ou vide"
              autoComplete="off"
              spellCheck={false}
            />
            <TextField
              label="Flag root"
              value={draft.rootFlag}
              onChange={(e) => patch({ rootFlag: e.target.value })}
              error={fieldErrors.rootFlag}
              placeholder="32 caractères hexadécimaux, ou vide"
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
            <span>Machine retirée (elle reste jouable, mais signalée comme ancienne)</span>
          </label>
          <label className="admin-check">
            <input
              type="checkbox"
              checked={draft.proOnly}
              onChange={(e) => patch({ proOnly: e.target.checked })}
            />
            <span>
              Réservée aux abonnés Pro (décochez-la pour en faire une machine d’initiation, visible et jouable
              sans abonnement)
            </span>
          </label>
          <div className="editor-footer">
            <span />
            <Button type="submit" icon="check" loading={saving} loadingLabel="Enregistrement…">
              {editing ? 'Enregistrer' : 'Publier la machine'}
            </Button>
          </div>
        </form>
      </Panel>

      <Panel title="Catalogue" description={`${boxes.length} machines publiées`}>
        {loading && boxes.length === 0 ? (
          <div className="empty">
            <Spinner size={22} label="Chargement du catalogue" />
          </div>
        ) : boxes.length === 0 ? (
          <p className="empty">Aucune machine publiée pour le moment.</p>
        ) : (
          <ul className="admin-list">
            {boxes.map((box) => (
              <li key={box.slug} className="admin-list__item">
                <span className="admin-list__text">
                  <span className="admin-list__title">
                    {box.name} {box.retired && <span className="badge">Retirée</span>}{' '}
                    {box.proOnly ? (
                      <span className="badge badge--locked">Pro</span>
                    ) : (
                      <span className="badge">Ouverte à tous</span>
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
                      Confirmer la suppression
                    </Button>
                    <Button variant="ghost" size="sm" onClick={() => setConfirming(null)}>
                      Annuler
                    </Button>
                  </span>
                ) : (
                  <span className="admin-list__actions">
                    <Button variant="ghost" size="sm" icon="target" onClick={() => edit(box)}>
                      Modifier
                    </Button>
                    <Button variant="ghost" size="sm" onClick={() => setConfirming(box.slug)}>
                      Supprimer
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
