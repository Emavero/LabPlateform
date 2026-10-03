import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from '@/app/App';
import { createContainer } from '@/di/container';
import { applyTheme, initialTheme } from '@/presentation/theme/theme';
import '@/presentation/styles/tokens.css';
import '@/presentation/styles/base.css';
import '@/presentation/styles/layout.css';
import '@/presentation/styles/components.css';
import '@/presentation/styles/pages.css';

// Avant le premier rendu : sans cela, une page choisie en clair s'afficherait
// en sombre le temps d'un battement.
applyTheme(initialTheme());

const root = document.getElementById('root');
if (!root) throw new Error('Élément #root introuvable.');

createRoot(root).render(
  <StrictMode>
    <App dependencies={createContainer()} />
  </StrictMode>,
);
