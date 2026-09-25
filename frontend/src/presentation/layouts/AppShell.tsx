import { useEffect, useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { Icon, Logo } from '../design-system';
import { Sidebar } from './Sidebar';

/** Gabarit des pages connectées : sidebar fixe sur grand écran, tiroir sur mobile. */
export function AppShell() {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const location = useLocation();

  useEffect(() => setDrawerOpen(false), [location.pathname]);

  useEffect(() => {
    if (!drawerOpen) return;
    const onKey = (event: KeyboardEvent) => event.key === 'Escape' && setDrawerOpen(false);
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [drawerOpen]);

  return (
    <div className="shell">
      <a className="skip-link" href="#main">
        Aller au contenu
      </a>
      <header className="topbar">
        <button
          type="button"
          className="icon-btn"
          onClick={() => setDrawerOpen((v) => !v)}
          aria-label={drawerOpen ? 'Fermer le menu' : 'Ouvrir le menu'}
          aria-expanded={drawerOpen}
        >
          <Icon name={drawerOpen ? 'x' : 'menu'} size={22} />
        </button>
        <Logo />
      </header>
      <Sidebar open={drawerOpen} onNavigate={() => setDrawerOpen(false)} />
      {drawerOpen && <div className="shell__scrim" onClick={() => setDrawerOpen(false)} aria-hidden="true" />}
      <main id="main" className="shell__main" tabIndex={-1}>
        <Outlet />
      </main>
    </div>
  );
}
