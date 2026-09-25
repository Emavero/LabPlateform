import { useState, type FormEvent } from 'react';
import { FLAG_LABELS, type FlagKind } from '@/domain/models/Box';
import { validateFlag } from '@/domain/validation/flag';
import { Alert, Button, Icon, TextField } from '../../design-system';

interface FlagFormProps {
  kind: FlagKind;
  points: number;
  owned: boolean;
  submitting: boolean;
  onSubmit: (kind: FlagKind, flag: string) => Promise<boolean>;
}

/**
 * Saisie d'un flag. La règle de format est vérifiée ici pour répondre tout
 * de suite ; seul le serveur sait si la valeur est la bonne.
 */
export function FlagForm({ kind, points, owned, submitting, onSubmit }: FlagFormProps) {
  const [value, setValue] = useState('');
  const [fieldError, setFieldError] = useState<string | undefined>();

  if (owned) {
    return (
      <div className="flag-form flag-form--owned">
        <Icon name="check" size={18} />
        <span>
          {FLAG_LABELS[kind]} validé — {points} points acquis.
        </span>
      </div>
    );
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const error = validateFlag(value);
    setFieldError(error);
    if (error) return;
    if (await onSubmit(kind, value)) setValue('');
  }

  return (
    <form className="flag-form" onSubmit={handleSubmit} noValidate>
      <TextField
        label={`${FLAG_LABELS[kind]} (${points} pts)`}
        name={`flag-${kind.toLowerCase()}`}
        value={value}
        onChange={(event) => setValue(event.target.value)}
        error={fieldError}
        hint="32 caractères hexadécimaux, tels qu'ils apparaissent sur la machine."
        autoComplete="off"
        spellCheck={false}
      />
      <Button type="submit" icon="flag" loading={submitting} loadingLabel="Vérification…">
        Soumettre
      </Button>
    </form>
  );
}

/** Message de succès d'une soumission : points gagnés, first blood, machine terminée. */
export function FlagSuccess({
  kind,
  points,
  firstBlood,
  pwned,
}: {
  kind: FlagKind;
  points: number;
  firstBlood: boolean;
  pwned: boolean;
}) {
  return (
    <Alert tone="success" title={`${FLAG_LABELS[kind]} accepté — +${points} points`}>
      {firstBlood && 'First blood : personne ne l’avait validé avant vous. '}
      {pwned
        ? 'La machine est possédée de bout en bout.'
        : 'Il reste un flag à trouver sur cette machine.'}
    </Alert>
  );
}
