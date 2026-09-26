import { useEffect, useState, type FormEvent } from 'react';
import { EMPTY_WRITEUP, type Writeup, type WriteupDraft } from '@/domain/models/Writeup';
import { Alert, Button, Icon, Panel, Spinner, TextField } from '../../design-system';
import { useWriteups } from '../../hooks/useWriteups';

/**
 * Comptes rendus d'une machine. L'éditeur n'apparaît qu'une fois la machine
 * possédée, et publier ne rend le texte lisible que par ceux qui l'ont
 * possédée aussi : la plateforme ne distribue pas les solutions.
 */
export function Writeups({ slug, pwned }: { slug: string; pwned: boolean }) {
  const state = useWriteups(slug);
  const [draft, setDraft] = useState<WriteupDraft>(EMPTY_WRITEUP);
  const [editing, setEditing] = useState(false);

  // Le compte rendu existant devient le brouillon dès qu'il arrive.
  useEffect(() => {
    if (state.mine) {
      setDraft({ title: state.mine.title, content: state.mine.content, published: state.mine.published });
    }
  }, [state.mine]);

  const others = state.writeups.filter((writeup) => !writeup.mine);
  const fieldErrors = state.error?.fieldErrors ?? {};

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (await state.save(draft)) setEditing(false);
  }

  return (
    <Panel
      title="Comptes rendus"
      description={
        pwned
          ? 'Publier partage votre méthode avec les joueurs qui ont possédé cette machine.'
          : 'Ils se lisent et s’écrivent une fois la machine possédée : pas de solution avant l’effort.'
      }
      actions={
        pwned &&
        !editing && (
          <Button variant="ghost" size="sm" icon="report" onClick={() => setEditing(true)}>
            {state.mine ? 'Modifier le mien' : 'Écrire le mien'}
          </Button>
        )
      }
    >
      {state.error && Object.keys(fieldErrors).length === 0 && <Alert tone="error">{state.error.message}</Alert>}

      {editing && (
        <form className="form" onSubmit={submit} noValidate>
          <TextField
            label="Titre"
            value={draft.title}
            onChange={(e) => setDraft({ ...draft, title: e.target.value })}
            error={fieldErrors.title}
          />
          <label className="field">
            <span className="field__label">Votre méthode</span>
            <textarea
              className="field__input editor-textarea editor-textarea--tall"
              rows={12}
              value={draft.content}
              onChange={(e) => setDraft({ ...draft, content: e.target.value })}
              placeholder="Reconnaissance, accès initial, élévation de privilèges, ce qui aurait dû l'empêcher."
            />
            {fieldErrors.content && <span className="field__error">{fieldErrors.content}</span>}
          </label>
          <label className="admin-check">
            <input
              type="checkbox"
              checked={draft.published}
              onChange={(e) => setDraft({ ...draft, published: e.target.checked })}
            />
            <span>Publier (lisible par les joueurs ayant possédé cette machine)</span>
          </label>
          <div className="editor-footer">
            <Button variant="ghost" onClick={() => setEditing(false)}>
              Annuler
            </Button>
            <Button type="submit" icon="check" loading={state.saving} loadingLabel="Enregistrement…">
              Enregistrer
            </Button>
          </div>
        </form>
      )}

      {state.loading && state.writeups.length === 0 ? (
        <div className="empty">
          <Spinner size={22} label="Chargement des comptes rendus" />
        </div>
      ) : (
        <div className="writeups">
          {state.mine && !editing && (
            <WriteupCard writeup={state.mine} onDelete={() => void state.remove()} />
          )}
          {others.map((writeup) => (
            <WriteupCard key={writeup.handle} writeup={writeup} />
          ))}
          {state.writeups.length === 0 && !editing && (
            <p className="empty">
              {pwned
                ? "Personne n'a encore publié de compte rendu. Le vôtre serait le premier."
                : 'Rien à afficher pour le moment.'}
            </p>
          )}
        </div>
      )}
    </Panel>
  );
}

function WriteupCard({ writeup, onDelete }: { writeup: Writeup; onDelete?: () => void }) {
  return (
    <article className={['writeup', writeup.mine && 'writeup--mine'].filter(Boolean).join(' ')}>
      <header className="writeup__header">
        <div>
          <h3 className="writeup__title">{writeup.title}</h3>
          <p className="writeup__meta">
            {writeup.mine ? 'Vous' : writeup.handle} · {writeup.updatedAt.toLocaleDateString('fr-FR')}
            {writeup.mine && !writeup.published && <span className="badge">Brouillon</span>}
          </p>
        </div>
        {onDelete && (
          <Button variant="ghost" size="sm" onClick={onDelete}>
            <Icon name="stop" size={14} /> Supprimer
          </Button>
        )}
      </header>
      <pre className="writeup__content">{writeup.content}</pre>
    </article>
  );
}
