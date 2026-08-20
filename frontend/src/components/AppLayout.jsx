import { useState } from 'react';
import { AppTopbar } from './AppTopbar';
import { Sidebar } from './Sidebar';

export function AppLayout({ children }) {
  const [sidebarOpen, setSidebarOpen] = useState(() =>
    typeof window !== 'undefined' ? window.innerWidth > 860 : true
  );

  return (
    <div className="app-shell">
      <AppTopbar onToggleSidebar={() => setSidebarOpen((open) => !open)} />

      <div className="app-body">
        <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />
        <main className="page">{children}</main>
      </div>
    </div>
  );
}
