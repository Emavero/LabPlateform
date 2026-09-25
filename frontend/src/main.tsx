import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from '@/app/App';
import { createContainer } from '@/di/container';
import '@/presentation/styles/tokens.css';
import '@/presentation/styles/base.css';
import '@/presentation/styles/layout.css';
import '@/presentation/styles/components.css';
import '@/presentation/styles/pages.css';

const root = document.getElementById('root');
if (!root) throw new Error('Élément #root introuvable.');

createRoot(root).render(
  <StrictMode>
    <App dependencies={createContainer()} />
  </StrictMode>,
);
