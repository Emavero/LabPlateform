import { useId, useState, type InputHTMLAttributes, type ReactNode } from 'react';
import { useI18n } from '../i18n/I18nContext';
import { Icon, type IconName } from './Icon';

interface TextFieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id'> {
  label: string;
  error?: string;
  hint?: ReactNode;
  icon?: IconName;
  /** Pour les champs mot de passe : bouton afficher/masquer. */
  revealable?: boolean;
}

export function TextField({ label, error, hint, icon, revealable = false, type = 'text', className, ...rest }: TextFieldProps) {
  const { t, tm } = useI18n();
  const id = useId();
  const [revealed, setRevealed] = useState(false);
  const describedBy = [error && `${id}-error`, hint && `${id}-hint`].filter(Boolean).join(' ') || undefined;
  const effectiveType = revealable && revealed ? 'text' : type;

  return (
    <div className={['field', error && 'field--invalid', className].filter(Boolean).join(' ')}>
      <label className="field__label" htmlFor={id}>
        {label}
      </label>
      <div className="field__control">
        {icon && <Icon name={icon} size={18} className="field__icon" />}
        <input
          id={id}
          type={effectiveType}
          className="field__input"
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy}
          {...rest}
        />
        {revealable && (
          <button
            type="button"
            className="field__reveal"
            onClick={() => setRevealed((v) => !v)}
            aria-label={t(revealed ? 'a11y.hidePassword' : 'a11y.showPassword')}
            aria-pressed={revealed}
          >
            <Icon name={revealed ? 'eyeOff' : 'eye'} size={18} />
          </button>
        )}
      </div>
      {error ? (
        <p className="field__error" id={`${id}-error`}>
          {tm(error)}
        </p>
      ) : hint ? (
        <p className="field__hint" id={`${id}-hint`}>
          {hint}
        </p>
      ) : null}
    </div>
  );
}
