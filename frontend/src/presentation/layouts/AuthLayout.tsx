import type { ReactNode } from 'react';
import { Logo } from '../design-system';

interface AuthLayoutProps {
  title: string;
  subtitle?: ReactNode;
  children: ReactNode;
  footer?: ReactNode;
}

/** Carte centrée en verre, commune à la connexion, l'inscription et la récupération. */
export function AuthLayout({ title, subtitle, children, footer }: AuthLayoutProps) {
  return (
    <div className="auth">
      <div className="auth__grid" aria-hidden="true" />
      <main className="auth__card">
        <div className="auth__brand">
          <Logo />
          <span className="auth__tag">Lab Platform</span>
        </div>
        <h1 className="auth__title">{title}</h1>
        {subtitle && <p className="auth__subtitle">{subtitle}</p>}
        {children}
        {footer && <div className="auth__footer">{footer}</div>}
      </main>
    </div>
  );
}
