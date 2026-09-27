import type { ReactNode } from 'react';
import { useI18n } from '../i18n/I18nContext';
import { Icon } from './Icon';

type Tone = 'error' | 'success' | 'info';

const ICONS = { error: 'alert', success: 'check', info: 'info' } as const;

interface AlertProps {
  tone?: Tone;
  title?: ReactNode;
  children?: ReactNode;
  action?: ReactNode;
}

export function Alert({ tone = 'info', title, children, action }: AlertProps) {
  const { tm } = useI18n();
  // Un texte déjà traduit par `t` ne figure pas au catalogue des messages et
  // ressort inchangé : seuls les messages d'erreur, formulés en français par le
  // domaine ou par le serveur, y trouvent leur traduction.
  const say = (node: ReactNode) => (typeof node === 'string' ? tm(node) : node);

  return (
    <div className={`alert alert--${tone}`} role={tone === 'error' ? 'alert' : 'status'}>
      <Icon name={ICONS[tone]} size={18} className="alert__icon" />
      <div className="alert__body">
        {title && <p className="alert__title">{say(title)}</p>}
        {children && <div className="alert__text">{say(children)}</div>}
      </div>
      {action && <div className="alert__action">{action}</div>}
    </div>
  );
}
