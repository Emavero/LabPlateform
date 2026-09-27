import { useState, type FormEvent } from 'react';
import type { FlagKind } from '@/domain/models/Box';
import { validateFlag } from '@/domain/validation/flag';
import { Alert, Button, Icon, TextField } from '../../design-system';
import { useI18n } from '../../i18n/I18nContext';

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
  const { t } = useI18n();
  const [value, setValue] = useState('');
  const [fieldError, setFieldError] = useState<string | undefined>();

  if (owned) {
    return (
      <div className="flag-form flag-form--owned">
        <Icon name="check" size={18} />
        <span>{t('flag.owned', { flag: t(`flag.${kind}`), points })}</span>
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
        label={t('flag.label', { flag: t(`flag.${kind}`), points })}
        name={`flag-${kind.toLowerCase()}`}
        value={value}
        onChange={(event) => setValue(event.target.value)}
        error={fieldError}
        hint={t('flag.hint')}
        autoComplete="off"
        spellCheck={false}
      />
      <Button type="submit" icon="flag" loading={submitting} loadingLabel={t('flag.checking')}>
        {t('flag.submit')}
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
  const { t } = useI18n();
  return (
    <Alert tone="success" title={t('flag.accepted', { flag: t(`flag.${kind}`), points })}>
      {firstBlood && `${t('flag.firstBlood')} `}
      {t(pwned ? 'flag.pwned' : 'flag.oneLeft')}
    </Alert>
  );
}
