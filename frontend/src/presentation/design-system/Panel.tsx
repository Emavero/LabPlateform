import type { HTMLAttributes, ReactNode } from 'react';

interface PanelProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  title?: ReactNode;
  eyebrow?: ReactNode;
  description?: ReactNode;
  /** Emplacement libre à droite de l'en-tête (boutons, badges). */
  actions?: ReactNode;
  footer?: ReactNode;
  as?: 'section' | 'article' | 'div';
  tone?: 'default' | 'accent';
}

/** Conteneur "verre" : l'en-tête et le pied sont des slots, le contenu est libre. */
export function Panel({
  title,
  eyebrow,
  description,
  actions,
  footer,
  as: Tag = 'section',
  tone = 'default',
  className,
  children,
  ...rest
}: PanelProps) {
  const hasHeader = title || eyebrow || description || actions;
  return (
    <Tag className={['panel', tone === 'accent' && 'panel--accent', className].filter(Boolean).join(' ')} {...rest}>
      {hasHeader && (
        <header className="panel__header">
          <div className="panel__heading">
            {eyebrow && <p className="panel__eyebrow">{eyebrow}</p>}
            {title && <h2 className="panel__title">{title}</h2>}
            {description && <p className="panel__description">{description}</p>}
          </div>
          {actions && <div className="panel__actions">{actions}</div>}
        </header>
      )}
      {children}
      {footer && <footer className="panel__footer">{footer}</footer>}
    </Tag>
  );
}
